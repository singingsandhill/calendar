package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주식 포지션 API
 */
@RestController
@RequestMapping("/api/stock/positions")
public class StockPositionApiController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockPositionService positionService;
    private final StockRepository stockRepository;

    public StockPositionApiController(StockPositionService positionService,
                                      StockRepository stockRepository) {
        this.positionService = positionService;
        this.stockRepository = stockRepository;
    }

    /**
     * 오픈 포지션 목록 조회
     */
    @GetMapping("/open")
    public ResponseEntity<List<PositionResponse>> getOpenPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        List<StockPosition> positions = positionService.getOpenPositions(tradingDate);

        Map<String, BigDecimal> priceMap = priceMap(positions, tradingDate);
        List<PositionResponse> response = positions.stream()
            .map(p -> toResponse(p, priceMap.get(p.getStockCode())))
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 청산된 포지션 목록 조회
     */
    @GetMapping("/closed")
    public ResponseEntity<List<PositionResponse>> getClosedPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        List<StockPosition> positions = positionService.getClosedPositions(tradingDate);

        Map<String, BigDecimal> priceMap = priceMap(positions, tradingDate);
        List<PositionResponse> response = positions.stream()
            .map(p -> toResponse(p, priceMap.get(p.getStockCode())))
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 포지션 조회
     */
    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAllPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        List<StockPosition> positions = positionService.getAllPositions(tradingDate);

        Map<String, BigDecimal> priceMap = priceMap(positions, tradingDate);
        List<PositionResponse> response = positions.stream()
            .map(p -> toResponse(p, priceMap.get(p.getStockCode())))
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * P&L 요약 조회
     */
    @GetMapping("/pnl/summary")
    public ResponseEntity<PnlSummaryResponse> getPnlSummary(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        List<StockPosition> allPositions = positionService.getAllPositions(tradingDate);
        List<StockPosition> closedPositions = positionService.getClosedPositions(tradingDate);

        BigDecimal totalRealizedPnl = closedPositions.stream()
            .map(StockPosition::getRealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long winCount = closedPositions.stream()
            .filter(p -> p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
            .count();

        long loseCount = closedPositions.stream()
            .filter(p -> p.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0)
            .count();

        BigDecimal winRate = closedPositions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(winCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(closedPositions.size()), 2, java.math.RoundingMode.HALF_UP);

        int openPositionCount = positionService.countOpenPositions(tradingDate);

        List<StockPosition> openPositions = positionService.getOpenPositions(tradingDate);
        Map<String, BigDecimal> priceMap = priceMap(openPositions, tradingDate);

        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalEntryAmount = BigDecimal.ZERO;
        for (StockPosition p : openPositions) {
            BigDecimal current = priceMap.get(p.getStockCode());
            if (current != null && p.getEntryPrice() != null && p.getRemainingQuantity() > 0) {
                BigDecimal diff = current.subtract(p.getEntryPrice());
                totalUnrealizedPnl = totalUnrealizedPnl.add(
                    diff.multiply(BigDecimal.valueOf(p.getRemainingQuantity())));
                totalEntryAmount = totalEntryAmount.add(
                    p.getEntryPrice().multiply(BigDecimal.valueOf(p.getRemainingQuantity())));
            }
        }

        BigDecimal unrealizedPnlPercent = totalEntryAmount.compareTo(BigDecimal.ZERO) > 0
            ? totalUnrealizedPnl.multiply(BigDecimal.valueOf(100))
                .divide(totalEntryAmount, 4, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ResponseEntity.ok(new PnlSummaryResponse(
            tradingDate,
            allPositions.size(),
            openPositionCount,
            closedPositions.size(),
            (int) winCount,
            (int) loseCount,
            winRate,
            totalRealizedPnl,
            totalUnrealizedPnl,
            unrealizedPnlPercent
        ));
    }

    private Map<String, BigDecimal> priceMap(List<StockPosition> positions, LocalDate tradingDate) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (StockPosition position : positions) {
            if (map.containsKey(position.getStockCode())) continue;
            stockRepository.findByStockCodeAndTradingDate(position.getStockCode(), tradingDate)
                .map(Stock::getCurrentPrice)
                .ifPresent(price -> map.put(position.getStockCode(), price));
        }
        return map;
    }

    private PositionResponse toResponse(StockPosition position, BigDecimal currentPrice) {
        BigDecimal unrealizedPnl = null;
        BigDecimal unrealizedPnlPct = null;
        if (currentPrice != null && position.getEntryPrice() != null
            && position.getRemainingQuantity() > 0) {
            BigDecimal diff = currentPrice.subtract(position.getEntryPrice());
            unrealizedPnl = diff.multiply(BigDecimal.valueOf(position.getRemainingQuantity()));
            if (position.getEntryPrice().compareTo(BigDecimal.ZERO) > 0) {
                unrealizedPnlPct = diff.multiply(BigDecimal.valueOf(100))
                    .divide(position.getEntryPrice(), 4, java.math.RoundingMode.HALF_UP);
            }
        }
        return new PositionResponse(
            position.getId(),
            position.getStockCode(),
            position.getStatus().name(),
            position.getEntryPrice(),
            position.getEntryQuantity(),
            position.getRemainingQuantity(),
            position.getAverageExitPrice(),
            position.getStopLossPrice(),
            position.getTrailingStopPrice(),
            position.getDayHighPrice(),
            position.isTp1Executed(),
            position.isTp2Executed(),
            position.isTp3Executed(),
            position.getRealizedPnl(),
            position.getCloseReason() != null ? position.getCloseReason().name() : null,
            position.getEnteredAt(),
            position.getClosedAt(),
            currentPrice,
            unrealizedPnl,
            unrealizedPnlPct
        );
    }

    // Response DTOs
    public record PositionResponse(
        Long id,
        String stockCode,
        String status,
        BigDecimal entryPrice,
        int entryQuantity,
        int remainingQuantity,
        BigDecimal avgExitPrice,
        BigDecimal stopLossPrice,
        BigDecimal trailingStopPrice,
        BigDecimal dayHighPrice,
        boolean tp1Executed,
        boolean tp2Executed,
        boolean tp3Executed,
        BigDecimal realizedPnl,
        String closeReason,
        LocalDateTime enteredAt,
        LocalDateTime closedAt,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent
    ) {}

    public record PnlSummaryResponse(
        LocalDate tradingDate,
        int totalPositions,
        int openPositions,
        int closedPositions,
        int winCount,
        int loseCount,
        BigDecimal winRate,
        BigDecimal totalRealizedPnl,
        BigDecimal totalUnrealizedPnl,
        BigDecimal unrealizedPnlPercent
    ) {}
}
