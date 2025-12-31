package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.domain.account.DailySummary;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trading")
public class TradeApiController {

    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final ProfitService profitService;
    private final TradingProperties tradingProperties;

    public TradeApiController(TradeRepository tradeRepository,
                              PositionRepository positionRepository,
                              ProfitService profitService,
                              TradingProperties tradingProperties) {
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.profitService = profitService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 거래 내역 조회
     */
    @GetMapping("/trades")
    public ResponseEntity<List<TradeDto>> getTrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String market = tradingProperties.getBot().getMarket();
        List<Trade> trades = tradeRepository.findByMarketOrderByCreatedAtDesc(market, page, size);

        List<TradeDto> tradeDtos = trades.stream()
                .map(t -> new TradeDto(
                        t.getId(),
                        t.getMarket(),
                        t.getTradeType().name(),
                        t.getUuid(),
                        t.getPrice() != null ? t.getPrice().doubleValue() : null,
                        t.getVolume() != null ? t.getVolume().doubleValue() : null,
                        t.getTotalAmount() != null ? t.getTotalAmount().doubleValue() : null,
                        t.getStatus().name(),
                        t.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(tradeDtos);
    }

    /**
     * 손익 요약 조회
     */
    @GetMapping("/profit/summary")
    public ResponseEntity<ProfitSummaryDto> getProfitSummary() {
        ProfitService.ProfitSummary summary = profitService.getProfitSummary();

        return ResponseEntity.ok(new ProfitSummaryDto(
                summary.totalValue().doubleValue(),
                summary.krwBalance().doubleValue(),
                summary.coinBalance().doubleValue(),
                summary.currentPrice().doubleValue(),
                summary.unrealizedPnl().doubleValue(),
                summary.unrealizedPnlPct().doubleValue(),
                summary.realizedPnl().doubleValue(),
                summary.totalTrades(),
                summary.winRate(),
                summary.avgPnlPct().doubleValue()
        ));
    }

    /**
     * 일별 손익 조회
     */
    @GetMapping("/profit/daily")
    public ResponseEntity<List<DailySummaryDto>> getDailyProfit(
            @RequestParam(defaultValue = "30") int days) {
        List<DailySummary> summaries = profitService.getDailySummaries(days);

        List<DailySummaryDto> dtos = summaries.stream()
                .map(s -> new DailySummaryDto(
                        s.getSummaryDate().toString(),
                        s.getStartBalance() != null ? s.getStartBalance().doubleValue() : 0,
                        s.getEndBalance() != null ? s.getEndBalance().doubleValue() : 0,
                        s.getRealizedPnl() != null ? s.getRealizedPnl().doubleValue() : 0,
                        s.getTradeCount(),
                        s.getWinCount(),
                        s.getTradeCount() > 0 ? (double) s.getWinCount() / s.getTradeCount() * 100 : 0
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * 포지션 목록 조회
     */
    @GetMapping("/positions")
    public ResponseEntity<List<PositionDto>> getPositions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String market = tradingProperties.getBot().getMarket();
        List<Position> positions = positionRepository.findByMarketOrderByOpenedAtDesc(market, page, size);

        List<PositionDto> dtos = positions.stream()
                .map(p -> new PositionDto(
                        p.getId(),
                        p.getMarket(),
                        p.getStatus().name(),
                        p.getEntryPrice() != null ? p.getEntryPrice().doubleValue() : null,
                        p.getEntryVolume() != null ? p.getEntryVolume().doubleValue() : null,
                        p.getExitPrice() != null ? p.getExitPrice().doubleValue() : null,
                        p.getRealizedPnl() != null ? p.getRealizedPnl().doubleValue() : null,
                        p.getRealizedPnlPct() != null ? p.getRealizedPnlPct().doubleValue() : null,
                        p.getCloseReason() != null ? p.getCloseReason().name() : null,
                        p.getOpenedAt() != null ? p.getOpenedAt().toString() : null,
                        p.getClosedAt() != null ? p.getClosedAt().toString() : null
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // Response DTOs
    public record TradeDto(
            Long id,
            String market,
            String type,
            String orderId,
            Double price,
            Double volume,
            Double amount,
            String status,
            String createdAt
    ) {}

    public record ProfitSummaryDto(
            double totalValue,
            double krwBalance,
            double coinBalance,
            double currentPrice,
            double unrealizedPnl,
            double unrealizedPnlPct,
            double realizedPnl,
            int totalTrades,
            double winRate,
            double avgPnlPct
    ) {}

    public record DailySummaryDto(
            String date,
            double startBalance,
            double endBalance,
            double realizedPnl,
            int tradeCount,
            int winCount,
            double winRate
    ) {}

    public record PositionDto(
            Long id,
            String market,
            String status,
            Double entryPrice,
            Double entryVolume,
            Double exitPrice,
            Double realizedPnl,
            Double realizedPnlPct,
            String closeReason,
            String openedAt,
            String closedAt
    ) {}
}
