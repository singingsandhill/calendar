package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.DivergenceResult;
import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DivergenceService {

    private static final int LOOKBACK_PERIOD = 20;  // 다이버전스 감지를 위한 캔들 수
    private static final int MIN_DISTANCE = 3;       // 피크/밸리 간 최소 거리

    private final CandleRepository candleRepository;
    private final IndicatorService indicatorService;
    private final TradingProperties tradingProperties;

    public DivergenceService(CandleRepository candleRepository,
                             IndicatorService indicatorService,
                             TradingProperties tradingProperties) {
        this.candleRepository = candleRepository;
        this.indicatorService = indicatorService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 다이버전스 감지
     */
    public DivergenceResult detect(String market) {
        List<Candle> candles = candleRepository.findByMarketOrderByDateTimeDesc(market, 100);

        if (candles.size() < LOOKBACK_PERIOD) {
            return DivergenceResult.none();
        }

        return new DivergenceResult(
                detectRsiDivergence(candles),
                detectStochasticDivergence(candles),
                detectVolumeDivergence(candles)
        );
    }

    /**
     * RSI 다이버전스 감지
     */
    private DivergenceType detectRsiDivergence(List<Candle> candles) {
        List<BigDecimal> prices = candles.stream().map(Candle::getTradePrice).toList();
        List<BigDecimal> rsiValues = calculateRsiSeries(candles);

        if (rsiValues.size() < LOOKBACK_PERIOD) {
            return DivergenceType.NONE;
        }

        return detectDivergence(prices, rsiValues);
    }

    /**
     * 스토캐스틱 다이버전스 감지
     */
    private DivergenceType detectStochasticDivergence(List<Candle> candles) {
        List<BigDecimal> prices = candles.stream().map(Candle::getTradePrice).toList();
        List<BigDecimal> stochValues = calculateStochasticSeries(candles);

        if (stochValues.size() < LOOKBACK_PERIOD) {
            return DivergenceType.NONE;
        }

        return detectDivergence(prices, stochValues);
    }

    /**
     * 거래량 다이버전스 감지
     */
    private DivergenceType detectVolumeDivergence(List<Candle> candles) {
        List<BigDecimal> prices = candles.stream().map(Candle::getTradePrice).toList();
        List<BigDecimal> volumes = candles.stream().map(Candle::getVolume).toList();

        if (volumes.size() < LOOKBACK_PERIOD) {
            return DivergenceType.NONE;
        }

        return detectDivergence(prices, volumes);
    }

    /**
     * 다이버전스 패턴 감지
     * 강세 다이버전스: 가격 Lower Low, 지표 Higher Low
     * 약세 다이버전스: 가격 Higher High, 지표 Lower High
     */
    private DivergenceType detectDivergence(List<BigDecimal> prices, List<BigDecimal> indicators) {
        // 가격 피크/밸리 찾기
        List<Integer> priceLows = findLocalMinima(prices, LOOKBACK_PERIOD);
        List<Integer> priceHighs = findLocalMaxima(prices, LOOKBACK_PERIOD);

        // 강세 다이버전스 체크 (저점 비교)
        if (priceLows.size() >= 2) {
            int recentLow = priceLows.get(0);
            int prevLow = priceLows.get(1);

            if (prevLow - recentLow >= MIN_DISTANCE) {
                // 가격: Lower Low (최근 저점이 이전 저점보다 낮음)
                boolean priceLowerLow = prices.get(recentLow).compareTo(prices.get(prevLow)) < 0;
                // 지표: Higher Low (최근 저점이 이전 저점보다 높음)
                boolean indicatorHigherLow = indicators.get(recentLow).compareTo(indicators.get(prevLow)) > 0;

                if (priceLowerLow && indicatorHigherLow) {
                    return DivergenceType.BULLISH;
                }
            }
        }

        // 약세 다이버전스 체크 (고점 비교)
        if (priceHighs.size() >= 2) {
            int recentHigh = priceHighs.get(0);
            int prevHigh = priceHighs.get(1);

            if (prevHigh - recentHigh >= MIN_DISTANCE) {
                // 가격: Higher High (최근 고점이 이전 고점보다 높음)
                boolean priceHigherHigh = prices.get(recentHigh).compareTo(prices.get(prevHigh)) > 0;
                // 지표: Lower High (최근 고점이 이전 고점보다 낮음)
                boolean indicatorLowerHigh = indicators.get(recentHigh).compareTo(indicators.get(prevHigh)) < 0;

                if (priceHigherHigh && indicatorLowerHigh) {
                    return DivergenceType.BEARISH;
                }
            }
        }

        return DivergenceType.NONE;
    }

    /**
     * 지역 최소값 (밸리) 찾기
     */
    private List<Integer> findLocalMinima(List<BigDecimal> values, int lookback) {
        List<Integer> minima = new ArrayList<>();

        for (int i = 1; i < Math.min(values.size() - 1, lookback); i++) {
            if (values.get(i).compareTo(values.get(i - 1)) < 0 &&
                values.get(i).compareTo(values.get(i + 1)) < 0) {
                minima.add(i);
            }
        }

        return minima;
    }

    /**
     * 지역 최대값 (피크) 찾기
     */
    private List<Integer> findLocalMaxima(List<BigDecimal> values, int lookback) {
        List<Integer> maxima = new ArrayList<>();

        for (int i = 1; i < Math.min(values.size() - 1, lookback); i++) {
            if (values.get(i).compareTo(values.get(i - 1)) > 0 &&
                values.get(i).compareTo(values.get(i + 1)) > 0) {
                maxima.add(i);
            }
        }

        return maxima;
    }

    /**
     * RSI 시계열 계산
     */
    private List<BigDecimal> calculateRsiSeries(List<Candle> candles) {
        List<BigDecimal> rsiValues = new ArrayList<>();
        int period = tradingProperties.getIndicators().getRsiPeriod();

        for (int i = 0; i < Math.min(candles.size() - period, LOOKBACK_PERIOD); i++) {
            List<Candle> subList = candles.subList(i, candles.size());
            BigDecimal rsi = indicatorService.calculateRSI(subList, period);
            if (rsi != null) {
                rsiValues.add(rsi);
            }
        }

        return rsiValues;
    }

    /**
     * 스토캐스틱 시계열 계산
     */
    private List<BigDecimal> calculateStochasticSeries(List<Candle> candles) {
        List<BigDecimal> stochValues = new ArrayList<>();
        int period = tradingProperties.getIndicators().getStochK();

        for (int i = 0; i < Math.min(candles.size() - period, LOOKBACK_PERIOD); i++) {
            List<Candle> subList = candles.subList(i, candles.size());
            BigDecimal stochK = indicatorService.calculateStochasticK(subList, period);
            if (stochK != null) {
                stochValues.add(stochK);
            }
        }

        return stochValues;
    }
}
