package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimeCalculator;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbDayCandleResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 일봉 레짐 서비스 (ADR trading/risk/0007) — 빗썸 일봉으로 BULL/BEAR/UNKNOWN 을 판정해 메모리에 캐시한다.
 *
 * <ul>
 *   <li>소스: {@code GET /v1/candles/days} 200개. 조회 실패 시에만 보관 1분봉(90일)의 일별 마지막 종가로 대체.</li>
 *   <li>형성 중인 당일봉은 판정에서 뺀다(ADR strategy/0009 정합) — 응답의 첫 원소가 당일봉이다.</li>
 *   <li>갱신: 하루 1회, KST 00:05 이후 첫 조회 때(일봉 확정 여유). 기동 후 첫 조회 때 로드. 실패하면 10분 뒤 재시도.</li>
 *   <li>전환 시 {@code REGIME_CHANGED} 이벤트(이전·새 상태, 종가, MA) 영속 저장.</li>
 * </ul>
 * 시각은 애플리케이션 {@link Clock} 빈(Asia/Seoul)을 쓴다.
 */
@Service
public class DailyRegimeService {

    private static final Logger log = LoggerFactory.getLogger(DailyRegimeService.class);

    private static final int DAY_CANDLE_COUNT = 200;          // 빗썸 1회 최대 — MA60 이어도 140일치 상태 경로
    private static final LocalTime DAILY_REFRESH_AFTER = LocalTime.of(0, 5);
    private static final Duration FAILURE_BACKOFF = Duration.ofMinutes(10);
    /** 최신 확정봉이 이보다 오래되면 판정을 믿지 않는다 (00:00~00:05 사이에는 그저께 봉이 최신일 수 있다). */
    private static final int MAX_STALE_DAYS = 2;

    private final BithumbApiClient bithumbApiClient;
    private final CandleRepository candleRepository;
    private final TradingProperties tradingProperties;
    private final TradingEventService tradingEventService;
    private final Clock clock;

    private List<RegimePoint> points = List.of();
    private LocalDate loadedOn;
    private Instant lastFailureAt;

    public DailyRegimeService(BithumbApiClient bithumbApiClient,
                              CandleRepository candleRepository,
                              TradingProperties tradingProperties,
                              TradingEventService tradingEventService,
                              Clock clock) {
        this.bithumbApiClient = bithumbApiClient;
        this.candleRepository = candleRepository;
        this.tradingProperties = tradingProperties;
        this.tradingEventService = tradingEventService;
        this.clock = clock;
    }

    /** 지금 적용되는 레짐 — 가장 최근 확정 일봉의 판정. 데이터가 없거나 낡았으면 UNKNOWN. */
    public synchronized MarketRegime currentRegime() {
        refreshIfDue();
        if (points.isEmpty()) {
            return MarketRegime.UNKNOWN;
        }
        RegimePoint latest = points.get(points.size() - 1);
        LocalDate today = LocalDate.now(clock);
        if (latest.date().isBefore(today.minusDays(MAX_STALE_DAYS))) {
            return MarketRegime.UNKNOWN;
        }
        return latest.regime();
    }

    /** day 에 적용되던 레짐 = day 전날 종가까지의 판정 (진입 시점에 미래를 보지 않음). 범위 밖이면 UNKNOWN. */
    public synchronized MarketRegime regimeInEffectOn(LocalDate day) {
        refreshIfDue();
        return RegimeCalculator.regimeInEffectOn(points, day);
    }

    /**
     * day 이후 레짐이 한 번이라도 바뀌었는지 — 비상 손절 뒤 리밸런싱 재매수 차단 해제 판정(구멍 2).
     * day 의 레짐을 알 수 없으면(이력 범위 밖) 바뀐 것으로 본다 — 확인할 수 없는 차단을 영구히 걸지 않는다.
     */
    /**
     * 포지션 시각(JVM 기본 시간대의 LocalDateTime — 운영 호스트 TZ 는 "추정 UTC")에 적용되는 레짐. 레짐 날짜는 KST(Clock)라
     * toLocalDate() 로 자르면 UTC 호스트에서 하루 9시간이 어긋난다 — 시각을 KST 로 옮긴 뒤 날짜를 잡는다.
     */
    public MarketRegime regimeInEffectAt(LocalDateTime jvmLocalTime) {
        return regimeInEffectOn(regimeDate(jvmLocalTime, ZoneId.systemDefault(), clock.getZone()));
    }

    /** {@link #hasRegimeChangedSince(LocalDate)} 의 포지션 시각 판 — 날짜 변환은 {@link #regimeInEffectAt} 과 같다. */
    public boolean hasRegimeChangedAfter(LocalDateTime jvmLocalTime) {
        return hasRegimeChangedSince(regimeDate(jvmLocalTime, ZoneId.systemDefault(), clock.getZone()));
    }

    static LocalDate regimeDate(LocalDateTime local, ZoneId localZone, ZoneId regimeZone) {
        return local.atZone(localZone).withZoneSameInstant(regimeZone).toLocalDate();
    }

    public synchronized boolean hasRegimeChangedSince(LocalDate day) {
        refreshIfDue();
        MarketRegime atDay = RegimeCalculator.regimeInEffectOn(points, day);
        if (atDay == MarketRegime.UNKNOWN) {
            return true;
        }
        LocalDate from = day.minusDays(1);
        return points.stream()
            .filter(p -> !p.date().isBefore(from))
            .anyMatch(p -> p.regime() != MarketRegime.UNKNOWN && p.regime() != atDay);
    }

    /** 스케줄러(KST 00:05)가 부르는 강제 갱신. */
    public synchronized void refresh() {
        load();
    }

    private void refreshIfDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (lastFailureAt != null && clock.instant().isBefore(lastFailureAt.plus(FAILURE_BACKOFF))) {
            return;
        }
        boolean neverLoaded = loadedOn == null;
        boolean newDay = loadedOn != null && loadedOn.isBefore(now.toLocalDate())
            && !now.toLocalTime().isBefore(DAILY_REFRESH_AFTER);
        if (neverLoaded || newDay) {
            load();
        }
    }

    private void load() {
        TradingProperties.Regime config = tradingProperties.getRegime();
        String market = tradingProperties.getBot().getMarket();
        LocalDate today = LocalDate.now(clock);

        List<DailyBar> bars = fromApi(market, today);
        if (bars.size() < config.getMaDays()) {
            log.warn("일봉 API 결과 {}개 < MA {}일 — 보관 1분봉 일별 종가로 대체", bars.size(), config.getMaDays());
            bars = fromStoredMinuteCandles(market, today);
        }
        if (bars.size() < config.getMaDays()) {
            // 직전 판정은 유지한다 — 일시 장애 한 번에 UNKNOWN 으로 떨어지지 않게. 낡은 판정(MAX_STALE_DAYS 초과)은
            // currentRegime 이 UNKNOWN 으로 본다.
            log.warn("일봉 레짐 데이터 부족({}개 < {}) — 직전 판정 유지, {}분 뒤 재시도", bars.size(), config.getMaDays(),
                FAILURE_BACKOFF.toMinutes());
            lastFailureAt = clock.instant();
            return;
        }

        MarketRegime before = points.isEmpty() ? null : points.get(points.size() - 1).regime();
        points = RegimeCalculator.compute(bars, config.getMaDays(), config.getHysteresisPct(), config.getMinDays());
        loadedOn = today;
        lastFailureAt = null;

        RegimePoint latest = points.get(points.size() - 1);
        log.info("일봉 레짐 {} (기준 {} 종가 {}, MA{} {})", latest.regime(), latest.date(), latest.close(),
            config.getMaDays(), latest.movingAverage());
        if (before != null && before != latest.regime()) {
            tradingEventService.record(TradingEventLevel.NOTICE, "REGIME_CHANGED", market,
                String.format("일봉 레짐 전환 %s → %s (%s 종가 %s, MA%d %s)", before, latest.regime(),
                    latest.date(), latest.close().toPlainString(), config.getMaDays(),
                    latest.movingAverage() != null ? latest.movingAverage().toPlainString() : "-"),
                String.format("{\"from\":\"%s\",\"to\":\"%s\",\"date\":\"%s\",\"close\":%s,\"ma\":%s}", before,
                    latest.regime(), latest.date(), latest.close().toPlainString(),
                    latest.movingAverage() != null ? latest.movingAverage().toPlainString() : "null"));
        }
    }

    /** 빗썸 일봉(최신순) → 확정봉 오름차순. 당일(형성 중) 봉과 그 이후 봉은 뺀다. */
    private List<DailyBar> fromApi(String market, LocalDate today) {
        List<BithumbDayCandleResponse> candles;
        try {
            candles = bithumbApiClient.getDayCandles(market, DAY_CANDLE_COUNT);
        } catch (Exception e) {
            log.warn("일봉 조회 실패: {}", e.getMessage());
            return List.of();
        }
        if (candles == null) {
            return List.of();
        }
        List<DailyBar> bars = new ArrayList<>();
        for (BithumbDayCandleResponse c : candles) {
            if (c.candleDateTimeKst() == null || c.tradePrice() == null) {
                continue;
            }
            LocalDate date = LocalDateTime.parse(c.candleDateTimeKst()).toLocalDate();
            if (!date.isBefore(today)) {
                continue;
            }
            bars.add(new DailyBar(date, decimal(c.openingPrice(), c.tradePrice()),
                decimal(c.highPrice(), c.tradePrice()), decimal(c.lowPrice(), c.tradePrice()),
                BigDecimal.valueOf(c.tradePrice())));
        }
        bars.sort(Comparator.comparing(DailyBar::date));
        return bars;
    }

    /** 보조 소스 — 보관 1분봉에서 날짜별 마지막 봉의 종가 (보관 기간만큼, 조회 실패 시에만). */
    private List<DailyBar> fromStoredMinuteCandles(String market, LocalDate today) {
        int days = tradingProperties.getBot().getCandleRetentionDays();
        List<DailyBar> bars = new ArrayList<>();
        for (int ago = days; ago >= 1; ago--) {
            LocalDate day = today.minusDays(ago);
            Optional<Candle> last = candleRepository.findLatestByMarketBefore(market, day.plusDays(1).atStartOfDay());
            if (last.isEmpty() || !last.get().getCandleDateTime().toLocalDate().equals(day)) {
                continue;
            }
            BigDecimal close = last.get().getTradePrice();
            bars.add(new DailyBar(day, close, close, close, close));
        }
        return bars;
    }

    private static BigDecimal decimal(Double value, Double fallback) {
        return BigDecimal.valueOf(value != null ? value : fallback);
    }
}
