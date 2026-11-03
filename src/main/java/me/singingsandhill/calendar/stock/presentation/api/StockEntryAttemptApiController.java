package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 진입 시도 조회 — 검증 단계({@link EntryAttempt}: 체결강도·호가 불균형·풀백 시간)와 주문 단계 실패
 * (Stock 의 사유·횟수·시각)를 한 응답으로 (ADR stock/infrastructure/0009).
 *
 * 51거래일 0체결 동안 주문 단계 실패 사유는 로그 이벤트에만 있어 DB 로 원인을 볼 수 없었다.
 */
@RestController
@RequestMapping("/api/stock/entry-attempts")
public class StockEntryAttemptApiController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EntryAttemptRepository entryAttemptRepository;
    private final StockRepository stockRepository;

    public StockEntryAttemptApiController(EntryAttemptRepository entryAttemptRepository,
                                          StockRepository stockRepository) {
        this.entryAttemptRepository = entryAttemptRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping
    public ResponseEntity<EntryAttemptsResponse> getEntryAttempts(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);

        List<ValidationAttemptResponse> validation = entryAttemptRepository.findByTradingDate(tradingDate)
            .stream()
            .map(a -> new ValidationAttemptResponse(a.getStockCode(), a.getAttemptedAt(), a.isAccepted(),
                a.getPassedConditions(), a.getRequiredConditions(), a.isStrengthPassed(),
                a.isImbalancePassed(), a.isTimePassed(), a.getCurrentPrice(), a.getPullbackLow(),
                a.getRejectReason()))
            .toList();

        List<OrderFailureResponse> orderFailures = stockRepository.findByTradingDate(tradingDate).stream()
            .filter(s -> s.getLastEntryFailReason() != null)
            .map(this::toOrderFailure)
            .toList();

        return ResponseEntity.ok(new EntryAttemptsResponse(tradingDate, validation, orderFailures));
    }

    private OrderFailureResponse toOrderFailure(Stock stock) {
        return new OrderFailureResponse(stock.getStockCode(), stock.getStockName(), stock.getState().name(),
            stock.getEntryFailCount(), stock.getLastEntryFailReason().name(), stock.getLastEntryFailAt());
    }

    public record EntryAttemptsResponse(
        LocalDate date,
        /** 진입 검증 단계 (ENTRY_READY 전환 직전) 통과·거절 */
        List<ValidationAttemptResponse> validation,
        /** 주문 단계 실패가 한 번이라도 있었던 종목 — ORDER_UNCONFIRMED 는 사유만 있고 횟수에 안 들어간다 */
        List<OrderFailureResponse> orderFailures
    ) {}

    public record ValidationAttemptResponse(
        String stockCode,
        LocalDateTime attemptedAt,
        boolean accepted,
        int passedConditions,
        int requiredConditions,
        boolean strengthPassed,
        boolean imbalancePassed,
        boolean timePassed,
        BigDecimal currentPrice,
        BigDecimal pullbackLow,
        String rejectReason
    ) {}

    public record OrderFailureResponse(
        String stockCode,
        String stockName,
        String state,
        int failCount,
        String lastReason,
        LocalDateTime lastFailAt
    ) {}
}
