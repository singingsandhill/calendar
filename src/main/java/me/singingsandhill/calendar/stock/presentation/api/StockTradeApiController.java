package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 주식 거래 내역 API
 */
@RestController
@RequestMapping("/api/stock/trades")
public class StockTradeApiController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockTradeRepository tradeRepository;

    public StockTradeApiController(StockTradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    /**
     * 당일 거래 내역 조회
     */
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "50") int limit) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
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

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
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
