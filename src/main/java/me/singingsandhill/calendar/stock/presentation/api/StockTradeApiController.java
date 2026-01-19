package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 주식 거래 내역 API
 */
@RestController
@RequestMapping("/api/stock/trades")
public class StockTradeApiController {

    private static final Logger log = LoggerFactory.getLogger(StockTradeApiController.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockTradeRepository tradeRepository;

    public StockTradeApiController(StockTradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    /**
     * 날짜 파라미터 파싱 (에러 처리 포함)
     */
    private LocalDate parseDateParameter(String date) {
        if (date == null) {
            return LocalDate.now(KST);
        }

        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format provided: '{}'. Expected format: yyyy-MM-dd", date);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Invalid date format: '%s'. Expected format: yyyy-MM-dd (e.g., 2026-01-17)", date)
            );
        }
    }

    /**
     * 당일 거래 내역 조회
     */
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "50") int limit) {

        LocalDate tradingDate = parseDateParameter(date);
        LocalDateTime from = tradingDate.atStartOfDay();
        LocalDateTime to = tradingDate.plusDays(1).atStartOfDay();

        List<StockTrade> trades = tradeRepository.findByOrderedAtBetween(from, to)
            .stream().limit(limit).toList();

        List<TradeResponse> response = trades.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 종목의 거래 내역 조회
     */
    @GetMapping("/{stockCode}")
    public ResponseEntity<List<TradeResponse>> getTradesByStockCode(
            @PathVariable String stockCode,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "50") int limit) {

        LocalDate tradingDate = parseDateParameter(date);
        LocalDateTime from = tradingDate.atStartOfDay();
        LocalDateTime to = tradingDate.plusDays(1).atStartOfDay();

        List<StockTrade> trades = tradeRepository.findByStockCodeAndOrderedAtBetween(stockCode, from, to)
            .stream().limit(limit).toList();

        List<TradeResponse> response = trades.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    private TradeResponse toResponse(StockTrade trade) {
        return new TradeResponse(
            trade.getId(),
            trade.getOrderId(),
            trade.getStockCode(),
            trade.getTradeType().name(),
            trade.getOrderType().name(),
            trade.getStatus().name(),
            trade.getQuantity(),
            trade.getOrderPrice(),
            trade.getExecutedPrice(),
            trade.getExecutedQuantity(),
            trade.getFee(),
            trade.getExitReason() != null ? trade.getExitReason().name() : null,
            trade.getCreatedAt(),
            trade.getExecutedAt()
        );
    }

    // Response DTO
    public record TradeResponse(
        Long id,
        String orderId,
        String stockCode,
        String tradeType,
        String orderType,
        String status,
        int quantity,
        BigDecimal orderPrice,
        BigDecimal executedPrice,
        int executedQuantity,
        BigDecimal fee,
        String exitReason,
        LocalDateTime createdAt,
        LocalDateTime executedAt
    ) {}
}
