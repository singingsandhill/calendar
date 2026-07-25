package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final StockPositionRepository positionRepository;
    private final EntryAttemptRepository entryAttemptRepository;
    private final StockMailService mailService;

    public DailyPerformanceReportService(StockPositionRepository positionRepository,
                                          EntryAttemptRepository entryAttemptRepository,
                                          StockMailService mailService) {
        this.positionRepository = positionRepository;
        this.entryAttemptRepository = entryAttemptRepository;
        this.mailService = mailService;
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
        List<StockPosition> positions = positionRepository.findByTradingDate(tradingDate);
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

        return new DailyReport(tradingDate, closed.size(), openCount, (int) winCount, (int) lossCount,
            winRate, totalPnl, attempts.size(), closeReasons, rejectReasons,
            closed.stream().collect(Collectors.toMap(StockPosition::getStockCode,
                p -> p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO,
                (a, b) -> a, LinkedHashMap::new)));
    }

    private static int signum(BigDecimal value) {
        return value == null ? 0 : value.signum();
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
        Map<String, BigDecimal> pnlByStock
    ) {
        public boolean hasActivity() {
            return closedCount > 0 || openCount > 0 || entryAttemptCount > 0;
        }
    }
}
