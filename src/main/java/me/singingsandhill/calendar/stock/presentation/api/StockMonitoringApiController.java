package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
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
 * 종목 모니터링 API
 */
@RestController
@RequestMapping("/api/stock/monitoring")
public class StockMonitoringApiController {

    private static final Logger log = LoggerFactory.getLogger(StockMonitoringApiController.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ScreeningService screeningService;

    public StockMonitoringApiController(ScreeningService screeningService) {
        this.screeningService = screeningService;
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
     * 관심종목(워치리스트) 조회
     */
    @GetMapping
    public ResponseEntity<List<StockMonitoringResponse>> getWatchlist(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<Stock> stocks = screeningService.getWatchlist(tradingDate);

        List<StockMonitoringResponse> response = stocks.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 활성 상태 종목 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<StockMonitoringResponse>> getActiveStocks(
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<Stock> stocks = screeningService.getActiveStocks(tradingDate);

        List<StockMonitoringResponse> response = stocks.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 상태의 종목 조회
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<List<StockMonitoringResponse>> getStocksByState(
            @PathVariable StockState state,
            @RequestParam(required = false) String date) {

        LocalDate tradingDate = parseDateParameter(date);
        List<Stock> stocks = screeningService.getStocksByState(tradingDate, state);

        List<StockMonitoringResponse> response = stocks.stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    private StockMonitoringResponse toResponse(Stock stock) {
        return new StockMonitoringResponse(
            stock.getId(),
            stock.getStockCode(),
            stock.getStockName(),
            stock.getState().name(),
            stock.getPrevClosePrice(),
            stock.getOpenPrice(),
            stock.getCurrentPrice(),
            stock.getHighPrice(),
            stock.getLowPrice(),
            stock.getGapPercent(),
            stock.getMarketCap(),
            stock.getTradeValue(),
            stock.getTradeStrength(),
            stock.getHighAfterOpen(),
            stock.getPullbackLow(),
            stock.calculateReturnFromOpen(),
            stock.getCreatedAt()
        );
    }

    // Response DTO
    public record StockMonitoringResponse(
        Long id,
        String stockCode,
        String stockName,
        String state,
        BigDecimal prevClosePrice,
        BigDecimal openPrice,
        BigDecimal currentPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal gapPercent,
        BigDecimal marketCap,
        BigDecimal tradeValue,
        BigDecimal tradeStrength,
        BigDecimal highAfterOpen,
        BigDecimal pullbackLow,
        BigDecimal returnFromOpen,
        LocalDateTime createdAt
    ) {}
}
