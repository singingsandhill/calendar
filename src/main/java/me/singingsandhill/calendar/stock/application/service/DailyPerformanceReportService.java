package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeMode;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 일일 실측 요약 (2026-07-24 리뷰 §9 / P2-5).
 *
 * PAPER 2~4주 실측으로 기대값을 판단하려면 매일의 진입·청산·손익·거절 사유가 한 곳에
 * 집계돼야 한다. 이 리포트가 LIVE 전환 판단(P2-5)의 근거 데이터가 된다.
 */
@Service
@Transactional(readOnly = true)
public class DailyPerformanceReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyPerformanceReportService.class);
    private static final String UNKNOWN_TYPE = "UNKNOWN";

    private final StockPositionRepository positionRepository;
    private final EntryAttemptRepository entryAttemptRepository;
    private final StockMailService mailService;
    private final StockRepository stockRepository;
    private final StockProperties stockProperties;

    public DailyPerformanceReportService(StockPositionRepository positionRepository,
                                          EntryAttemptRepository entryAttemptRepository,
                                          StockMailService mailService,
                                          StockRepository stockRepository,
                                          StockProperties stockProperties) {
        this.positionRepository = positionRepository;
        this.entryAttemptRepository = entryAttemptRepository;
        this.mailService = mailService;
        this.stockRepository = stockRepository;
        this.stockProperties = stockProperties;
    }

    /**
     * 당일 실적 집계 후 메일 발송 + 이벤트 기록.
     * 매매도 진입 시도도 없던 날은 알림 피로를 피하기 위해 발송하지 않는다.
     */
    public void sendDailyReport(LocalDate tradingDate) {
        DailyReport report = buildReport(tradingDate);

        log.info("일일 실적 {} — 청산 {}건(승 {} / 패 {}), 승률 {}%, 실현손익 {}, 미청산 {}건, 진입시도 {}건",
            tradingDate, report.closedCount(), report.winCount(), report.lossCount(),
            report.winRatePercent(), report.totalRealizedPnl(), report.openCount(),
            report.entryAttemptCount());

        TradeEvents.event("DAILY_REPORT")
            .with("tradingDate", tradingDate)
            .with("closed", report.closedCount())
            .with("win", report.winCount())
            .with("loss", report.lossCount())
            .with("winRate", report.winRatePercent())
            .with("realizedPnl", report.totalRealizedPnl())
            .with("open", report.openCount())
            .with("attempts", report.entryAttemptCount())
            .with("entryFailures", report.entryFailureCounts())
            .with("productTypes", report.productTypeBreakdown())
            .log();

        if (!report.hasActivity()) {
            log.debug("매매·진입 시도가 없어 일일 리포트 메일을 생략한다 ({})", tradingDate);
            return;
        }

        try {
            mailService.sendDailyPerformanceReport(report);
        } catch (Exception e) {
            log.error("일일 리포트 메일 발송 실패: {}", e.getMessage());
        }
    }

    DailyReport buildReport(LocalDate tradingDate) {
        // 현재 봇 모드의 포지션만 — PAPER 실측과 LIVE 손익을 한 리포트에 합산하지 않는다 (ADR stock/observability/0003)
        StockTradeMode mode = StockTradeMode.valueOf(stockProperties.getBot().getMode().name());
        List<StockPosition> positions = positionRepository.findByTradingDate(tradingDate).stream()
            .filter(p -> mode.includes(p.getMode()))
            .toList();
        List<EntryAttempt> attempts = entryAttemptRepository.findByTradingDate(tradingDate);

        List<StockPosition> closed = positions.stream().filter(StockPosition::isClosed).toList();
        int openCount = positions.size() - closed.size();

        long winCount = closed.stream().filter(p -> signum(p.getRealizedPnl()) > 0).count();
        long lossCount = closed.stream().filter(p -> signum(p.getRealizedPnl()) < 0).count();

        BigDecimal totalPnl = closed.stream()
            .map(StockPosition::getRealizedPnl)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal winRate = closed.isEmpty() ? BigDecimal.ZERO
            : BigDecimal.valueOf(winCount).multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(closed.size()), 2, RoundingMode.HALF_UP);

        Map<String, Long> closeReasons = closed.stream()
            .filter(p -> p.getCloseReason() != null)
            .collect(Collectors.groupingBy(p -> p.getCloseReason().name(),
                LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> rejectReasons = attempts.stream()
            .filter(a -> !a.isAccepted() && a.getRejectReason() != null)
            .collect(Collectors.groupingBy(EntryAttempt::getRejectReason,
                LinkedHashMap::new, Collectors.counting()));

        List<Stock> stocks = stockRepository.findByTradingDate(tradingDate);
        // 주문 단계 진입 실패 — 종목별 마지막 사유로 센다 (ADR stock/infrastructure/0009)
        Map<String, Long> entryFailures = stocks.stream()
            .filter(s -> s.getLastEntryFailReason() != null)
            .collect(Collectors.groupingBy(s -> s.getLastEntryFailReason().name(),
                LinkedHashMap::new, Collectors.counting()));

        return new DailyReport(tradingDate, closed.size(), openCount, (int) winCount, (int) lossCount,
            winRate, totalPnl, attempts.size(), closeReasons, rejectReasons, entryFailures,
            closed.stream().collect(Collectors.toMap(StockPosition::getStockCode,
                p -> p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO,
                (a, b) -> a, LinkedHashMap::new)), productTypeBreakdown(stocks, attempts, closed));
    }

    /**
     * 상품 유형별 감시·진입 시도·청산·승·실현손익 (ADR stock/algorithm/0011) — ETF·ETN 제외는 데이터가 반대 방향이라
     * 보류했고, PAPER 체결이 쌓이면 이 분해로 재평가한다. 유형은 그날 종목 행에서 찾고, 모르면 UNKNOWN.
     */
    private static Map<String, ProductTypeBreakdown> productTypeBreakdown(List<Stock> stocks, List<EntryAttempt> attempts,
                                                                        List<StockPosition> closed) {
        Map<String, String> typeByCode = new HashMap<>();
        stocks.forEach(s -> typeByCode.putIfAbsent(s.getStockCode(), typeKey(s.getProductType())));
        Function<String, String> typeOf = code -> typeByCode.getOrDefault(code, UNKNOWN_TYPE);

        List<String> keys = new ArrayList<>();
        for (StockProductType type : StockProductType.values()) {
            keys.add(type.name());
        }
        keys.add(UNKNOWN_TYPE);
        Map<String, ProductTypeBreakdown> breakdown = new LinkedHashMap<>();
        for (String key : keys) {
            int watched = (int) stocks.stream().filter(s -> key.equals(typeKey(s.getProductType()))).count();
            int tried = (int) attempts.stream().filter(a -> key.equals(typeOf.apply(a.getStockCode()))).count();
            List<StockPosition> mine = closed.stream().filter(p -> key.equals(typeOf.apply(p.getStockCode()))).toList();
            if (watched + tried + mine.size() == 0) {
                continue;
            }
            breakdown.put(key, new ProductTypeBreakdown(watched, tried, mine.size(),
                (int) mine.stream().filter(p -> signum(p.getRealizedPnl()) > 0).count(),
                mine.stream().map(StockPosition::getRealizedPnl).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return breakdown;
    }

    private static String typeKey(StockProductType type) {
        return type != null ? type.name() : UNKNOWN_TYPE;
    }

    private static int signum(BigDecimal value) {
        return value == null ? 0 : value.signum();
    }

    /** 상품 유형 하나의 하루 — 감시 종목 수, 진입 시도 수, 청산 수, 승, 실현손익. */
    public record ProductTypeBreakdown(int watched, int attempts, int closed, int wins, BigDecimal realizedPnl) {
    }

    /** 하루치 실적 스냅샷. */
    public record DailyReport(
        LocalDate tradingDate,
        int closedCount,
        int openCount,
        int winCount,
        int lossCount,
        BigDecimal winRatePercent,
        BigDecimal totalRealizedPnl,
        int entryAttemptCount,
        Map<String, Long> closeReasonCounts,
        Map<String, Long> rejectReasonCounts,
        /** 주문 단계 진입 실패 사유 → 종목 수 (ADR stock/infrastructure/0009) */
        Map<String, Long> entryFailureCounts,
        Map<String, BigDecimal> pnlByStock,
        /** 상품 유형(STOCK/ETF/ETN/UNKNOWN) → 감시·시도·청산·손익 (ADR stock/algorithm/0011) */
        Map<String, ProductTypeBreakdown> productTypeBreakdown
    ) {
        public boolean hasActivity() {
            return closedCount > 0 || openCount > 0 || entryAttemptCount > 0 || !entryFailureCounts.isEmpty();
        }
    }
}
