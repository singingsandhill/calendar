package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
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
 * 주식 포지션 API
 */
@RestController
@RequestMapping("/api/stock/positions")
public class StockPositionApiController {

    private static final Logger log = LoggerFactory.getLogger(StockPositionApiController.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockPositionService positionService;

    public StockPositionApiController(StockPositionService positionService) {
        this.positionService = positionService;
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
     * 오픈 포지션 목록 조회
     */
    @GetMapping("/open")
    public ResponseEntity<List<PositionResponse>> getOpenPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<StockPosition> positions = positionService.getOpenPositions(tradingDate);

        List<PositionResponse> response = positions.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 청산된 포지션 목록 조회
     */
    @GetMapping("/closed")
    public ResponseEntity<List<PositionResponse>> getClosedPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<StockPosition> positions = positionService.getClosedPositions(tradingDate);

        List<PositionResponse> response = positions.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 포지션 조회
     */
    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAllPositions(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<StockPosition> positions = positionService.getAllPositions(tradingDate);

        List<PositionResponse> response = positions.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * P&L 요약 조회
     */
    @GetMapping("/pnl/summary")
    public ResponseEntity<PnlSummaryResponse> getPnlSummary(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
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

        return ResponseEntity.ok(new PnlSummaryResponse(
            tradingDate,
            allPositions.size(),
            openPositionCount,
            closedPositions.size(),
            (int) winCount,
            (int) loseCount,
            winRate,
            totalRealizedPnl
        ));
    }

    private PositionResponse toResponse(StockPosition position) {
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
            position.getClosedAt()
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
        LocalDateTime closedAt
    ) {}

    public record PnlSummaryResponse(
        LocalDate tradingDate,
        int totalPositions,
        int openPositions,
        int closedPositions,
        int winCount,
        int loseCount,
        BigDecimal winRate,
        BigDecimal totalRealizedPnl
    ) {}
}
