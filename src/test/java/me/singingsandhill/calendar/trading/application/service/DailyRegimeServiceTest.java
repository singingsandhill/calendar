package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbDayCandleResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 일봉 레짐 서비스 — 빗썸 일봉 로드·형성봉 제외·캐시·하루 1회 갱신·전환 이벤트·조회 실패 보조 소스 (ADR trading/risk/0007).
 */
class DailyRegimeServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String MARKET = "KRW-ADA";

    private final BithumbApiClient api = mock(BithumbApiClient.class);
    private final CandleRepository candleRepository = mock(CandleRepository.class);
    private final TradingEventService events = mock(TradingEventService.class);

    /** 가변 시계 — 날짜를 넘겨 하루 1회 갱신을 검증한다. */
    private static final class MutableClock extends Clock {
        Instant now;
        MutableClock(LocalDateTime kst) { set(kst); }
        void set(LocalDateTime kst) { now = kst.atZone(KST).toInstant(); }
        @Override public ZoneId getZone() { return KST; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static TradingProperties props() {
        TradingProperties props = new TradingProperties();
        props.getRegime().setMaDays(3);
        props.getRegime().setHysteresisPct(0.0);
        props.getRegime().setMinDays(1);
        return props;
    }

    /** 오래된 것부터 closes, 마지막 원소가 today(형성 중) 봉. 응답은 빗썸처럼 최신순. */
    private static List<BithumbDayCandleResponse> dayCandles(LocalDate today, double... closesOldestFirst) {
        List<BithumbDayCandleResponse> asc = new ArrayList<>();
        int n = closesOldestFirst.length;
        for (int i = 0; i < n; i++) {
            LocalDate d = today.minusDays(n - 1 - i);
            double c = closesOldestFirst[i];
            asc.add(new BithumbDayCandleResponse(MARKET, d.atStartOfDay().minusHours(9).toString(),
                d.atStartOfDay().toString(), c, c, c, c));
        }
        List<BithumbDayCandleResponse> desc = new ArrayList<>(asc);
        java.util.Collections.reverse(desc);
        return desc;
    }

    @Test
    void excludesFormingBarOfToday() {
        // 마지막(오늘) 봉이 폭락해도 형성 중이라 판정에 쓰지 않는다 — 어제까지 BULL
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        when(api.getDayCandles(eq(MARKET), anyInt())).thenReturn(dayCandles(today, 100, 100, 103, 104, 10));
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        assertThat(service.currentRegime()).isEqualTo(MarketRegime.BULL);
    }

    @Test
    void cachesWithinTheDayAndRefreshesAfterFiveMinutesPastMidnight() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        when(api.getDayCandles(eq(MARKET), anyInt())).thenReturn(dayCandles(today, 100, 100, 103, 104, 104));
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        service.currentRegime();
        service.currentRegime();
        clock.set(today.plusDays(1).atTime(0, 3));   // 00:05 전 — 아직 어제 판정 유지
        service.currentRegime();
        verify(api, times(1)).getDayCandles(eq(MARKET), anyInt());

        clock.set(today.plusDays(1).atTime(0, 6));
        service.currentRegime();
        verify(api, times(2)).getDayCandles(eq(MARKET), anyInt());
    }

    @Test
    void recordsRegimeChangedEventWhenStateFlips() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        when(api.getDayCandles(eq(MARKET), anyInt()))
            .thenReturn(dayCandles(today, 100, 100, 103, 104, 104))          // BULL
            .thenReturn(dayCandles(today.plusDays(1), 100, 103, 104, 60, 60)); // 어제 종가 60 → BEAR
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        assertThat(service.currentRegime()).isEqualTo(MarketRegime.BULL);
        verify(events, never()).record(any(), eq("REGIME_CHANGED"), anyString(), anyString(), any());

        clock.set(today.plusDays(1).atTime(0, 10));
        assertThat(service.currentRegime()).isEqualTo(MarketRegime.BEAR);
        verify(events).record(eq(TradingEventLevel.NOTICE), eq("REGIME_CHANGED"), eq(MARKET),
            contains("BULL → BEAR"), contains("\"to\":\"BEAR\""));
    }

    @Test
    void fallsBackToStoredMinuteCandlesWhenApiFails() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        when(api.getDayCandles(eq(MARKET), anyInt())).thenReturn(List.of());
        // 각 날짜의 마지막 1분봉 종가: 100, 100, 103, 104 (오늘은 제외)
        double[] closes = {100, 100, 103, 104};
        when(candleRepository.findLatestByMarketBefore(eq(MARKET), any())).thenAnswer(inv -> {
            LocalDateTime before = inv.getArgument(1);
            LocalDate day = before.toLocalDate().minusDays(1);
            long ago = java.time.temporal.ChronoUnit.DAYS.between(day, today);   // 1..4
            if (ago < 1 || ago > closes.length) return Optional.empty();
            double c = closes[(int) (closes.length - ago)];
            BigDecimal p = BigDecimal.valueOf(c);
            return Optional.of(new Candle(null, MARKET, day.atTime(23, 59), p, p, p, p, BigDecimal.ONE,
                BigDecimal.ONE, LocalDateTime.now()));
        });
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        assertThat(service.currentRegime()).isEqualTo(MarketRegime.BULL);
    }

    @Test
    void unknownWhenBothSourcesFailAndRetriesOnlyAfterBackoff() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        when(api.getDayCandles(eq(MARKET), anyInt())).thenReturn(List.of());
        when(candleRepository.findLatestByMarketBefore(eq(MARKET), any())).thenReturn(Optional.empty());
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        assertThat(service.currentRegime()).isEqualTo(MarketRegime.UNKNOWN);
        clock.set(today.atTime(10, 5));
        service.currentRegime();                                    // 백오프(10분) 안 — 재조회 안 함
        verify(api, times(1)).getDayCandles(eq(MARKET), anyInt());
        clock.set(today.atTime(10, 11));
        service.currentRegime();
        verify(api, times(2)).getDayCandles(eq(MARKET), anyInt());
    }

    @Test
    void regimeInEffectOnPastDayUsesPriorClose() {
        LocalDate today = LocalDate.of(2026, 10, 22);
        MutableClock clock = new MutableClock(today.atTime(10, 0));
        // d-4..d-1 = 100,100,103,60 + 오늘. MA3: d-2 BULL, d-1 BEAR
        when(api.getDayCandles(eq(MARKET), anyInt())).thenReturn(dayCandles(today, 100, 100, 103, 60, 60));
        DailyRegimeService service = new DailyRegimeService(api, candleRepository, props(), events, clock);

        assertThat(service.regimeInEffectOn(today.minusDays(1))).isEqualTo(MarketRegime.BULL);
        assertThat(service.regimeInEffectOn(today)).isEqualTo(MarketRegime.BEAR);
    }

    /**
     * 포지션 시각은 JVM 기본 시간대의 LocalDateTime 이고(운영 호스트 TZ 는 "추정 UTC") 레짐 날짜는 KST 다 — UTC 16:30 은 KST 다음 날
     * 01:30 이라 toLocalDate() 로 자르면 하루가 어긋난다 (최종 리뷰 발견, ADR trading/risk/0007).
     */
    @Test
    void positionTimestampsAreConvertedToTheRegimeZoneBeforeTakingTheDate() {
        assertThat(DailyRegimeService.regimeDate(LocalDateTime.of(2026, 10, 20, 16, 30), ZoneId.of("UTC"), ZoneId.of("Asia/Seoul")))
            .isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(DailyRegimeService.regimeDate(LocalDateTime.of(2026, 10, 20, 16, 30), ZoneId.of("Asia/Seoul"),
            ZoneId.of("Asia/Seoul"))).isEqualTo(LocalDate.of(2026, 10, 20));
    }
}
