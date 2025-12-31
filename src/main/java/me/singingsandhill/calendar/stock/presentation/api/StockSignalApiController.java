package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 주식 시그널 API (감사/디버깅용)
 */
@RestController
@RequestMapping("/api/stock/signals")
public class StockSignalApiController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockSignalRepository signalRepository;

    public StockSignalApiController(StockSignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    /**
     * 당일 시그널 내역 조회
     */
    @GetMapping
    public ResponseEntity<List<SignalResponse>> getSignals(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "100") int limit) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        LocalDateTime from = tradingDate.atStartOfDay();
        LocalDateTime to = tradingDate.plusDays(1).atStartOfDay();

        List<StockSignal> signals = signalRepository.findBySignalTimeBetween(from, to)
            .stream().limit(limit).toList();

        List<SignalResponse> response = signals.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 종목의 시그널 내역 조회
     */
    @GetMapping("/{stockCode}")
    public ResponseEntity<List<SignalResponse>> getSignalsByStockCode(
            @PathVariable String stockCode,
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        LocalDateTime from = tradingDate.atStartOfDay();
        LocalDateTime to = tradingDate.plusDays(1).atStartOfDay();

        List<StockSignal> signals = signalRepository.findByStockCodeAndSignalTimeBetween(stockCode, from, to);

        List<SignalResponse> response = signals.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    private SignalResponse toResponse(StockSignal signal) {
        return new SignalResponse(
            signal.getId(),
            signal.getStockCode(),
            signal.getSignalType().name(),
            signal.getCurrentPrice(),
            signal.getGapPercent(),
            signal.getHighPrice(),
            signal.getPullbackPercent(),
            signal.getBouncePercent(),
            signal.isExecuted(),
            signal.getSignalTime(),
            signal.getCreatedAt()
        );
    }

    // Response DTO
    public record SignalResponse(
        Long id,
        String stockCode,
        String signalType,
        BigDecimal price,
        BigDecimal gapPercent,
        BigDecimal highPrice,
        BigDecimal pullbackPercent,
        BigDecimal bouncePercent,
        boolean executed,
        LocalDateTime signalTime,
        LocalDateTime createdAt
    ) {}
}
