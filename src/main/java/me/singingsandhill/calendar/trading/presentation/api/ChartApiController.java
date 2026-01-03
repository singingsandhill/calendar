package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.application.service.CandleService;
import me.singingsandhill.calendar.trading.application.service.IndicatorService;
import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading")
public class ChartApiController {

    private final CandleService candleService;
    private final IndicatorService indicatorService;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;
    private final TradeRepository tradeRepository;

    public ChartApiController(CandleService candleService,
                              IndicatorService indicatorService,
                              BithumbApiClient bithumbApiClient,
                              TradingProperties tradingProperties,
                              TradeRepository tradeRepository) {
        this.candleService = candleService;
        this.indicatorService = indicatorService;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
        this.tradeRepository = tradeRepository;
    }

    /**
     * 캔들 데이터 조회 (차트용)
     */
    @GetMapping("/candles")
    public ResponseEntity<CandleDataResponse> getCandles(
            @RequestParam(defaultValue = "200") int count) {
        List<Candle> candles = candleService.getLatestCandles(count);
        String market = tradingProperties.getBot().getMarket();
        IndicatorResult indicators = indicatorService.calculate(market);

        List<CandleDto> candleDtos = candles.stream()
                .map(c -> new CandleDto(
                        c.getCandleDateTime().toString(),
                        c.getOpeningPrice().doubleValue(),
                        c.getHighPrice().doubleValue(),
                        c.getLowPrice().doubleValue(),
                        c.getTradePrice().doubleValue(),
                        c.getVolume().doubleValue()
                ))
                .toList();

        IndicatorDto indicatorDto = indicators != null ? new IndicatorDto(
                indicators.ma5() != null ? indicators.ma5().doubleValue() : null,
                indicators.ma20() != null ? indicators.ma20().doubleValue() : null,
                indicators.ma60() != null ? indicators.ma60().doubleValue() : null,
                indicators.rsi() != null ? indicators.rsi().doubleValue() : null,
                indicators.stochK() != null ? indicators.stochK().doubleValue() : null,
                indicators.stochD() != null ? indicators.stochD().doubleValue() : null
        ) : null;

        return ResponseEntity.ok(new CandleDataResponse(candleDtos, indicatorDto));
    }

    /**
     * 실시간 현재가 조회
     */
    @GetMapping("/ticker")
    public ResponseEntity<Map<String, Object>> getTicker() {
        String market = tradingProperties.getBot().getMarket();
        Double currentPrice = bithumbApiClient.getCurrentPrice();
        IndicatorResult indicators = indicatorService.calculate(market);

        Map<String, Object> response = new HashMap<>();
        response.put("market", market);
        response.put("currentPrice", currentPrice);

        if (indicators != null) {
            response.put("ma5", indicators.ma5());
            response.put("ma20", indicators.ma20());
            response.put("ma60", indicators.ma60());
            response.put("rsi", indicators.rsi());
            response.put("stochK", indicators.stochK());
            response.put("stochD", indicators.stochD());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 차트 오버레이용 거래 마커 데이터 조회
     */
    @GetMapping("/chart/trades")
    public ResponseEntity<List<TradeMarkerDto>> getTradesForChart(
            @RequestParam(defaultValue = "200") int minutes) {
        String market = tradingProperties.getBot().getMarket();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(minutes);

        List<Trade> trades = tradeRepository.findByMarketAndCreatedAtBetween(market, start, end);

        List<TradeMarkerDto> markers = trades.stream()
                .filter(t -> t.getStatus() == TradeStatus.DONE)
                .map(t -> new TradeMarkerDto(
                        t.getCreatedAt().toString(),
                        t.getTradeType().name(),
                        t.getExecutedPrice() != null ? t.getExecutedPrice().doubleValue()
                                : (t.getPrice() != null ? t.getPrice().doubleValue() : 0),
                        t.getExecutedVolume() != null ? t.getExecutedVolume().doubleValue()
                                : (t.getVolume() != null ? t.getVolume().doubleValue() : 0),
                        t.getFee() != null ? t.getFee().doubleValue() : 0
                ))
                .toList();

        return ResponseEntity.ok(markers);
    }

    // Response DTOs
    public record CandleDataResponse(List<CandleDto> candles, IndicatorDto indicators) {}

    public record CandleDto(
            String time,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {}

    public record IndicatorDto(
            Double ma5,
            Double ma20,
            Double ma60,
            Double rsi,
            Double stochK,
            Double stochD
    ) {}

    public record TradeMarkerDto(
            String time,
            String type,
            double price,
            double volume,
            double fee
    ) {}
}
