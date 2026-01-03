package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class IndicatorService {

    private final CandleRepository candleRepository;
    private final TradingProperties tradingProperties;

    public IndicatorService(CandleRepository candleRepository, TradingProperties tradingProperties) {
        this.candleRepository = candleRepository;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 기술적 지표 계산
     */
    public IndicatorResult calculate(String market) {
        int requiredCandles = Math.max(tradingProperties.getIndicators().getMaLong(),
                tradingProperties.getIndicators().getRsiPeriod()) + 20;
        List<Candle> candles = candleRepository.findByMarketOrderByDateTimeDesc(market, requiredCandles);

        if (candles.isEmpty()) {
            return null;
        }

        BigDecimal currentPrice = candles.get(0).getTradePrice();
        BigDecimal currentVolume = candles.get(0).getVolume();
        int rsiTrend = calculateRsiTrend(candles, tradingProperties.getIndicators().getRsiPeriod());

        return new IndicatorResult(
                currentPrice,
                calculateMA(candles, tradingProperties.getIndicators().getMaShort()),
                calculateMA(candles, tradingProperties.getIndicators().getMaMid()),
                calculateMA(candles, tradingProperties.getIndicators().getMaLong()),
                calculateRSI(candles, tradingProperties.getIndicators().getRsiPeriod()),
                calculateStochasticK(candles, tradingProperties.getIndicators().getStochK()),
                calculateStochasticD(candles, tradingProperties.getIndicators().getStochK(),
                        tradingProperties.getIndicators().getStochD()),
                calculateMA(candles.stream()
                        .map(c -> new Candle(c.getId(), c.getMarket(), c.getCandleDateTime(),
                                c.getVolume(), c.getVolume(), c.getVolume(), c.getVolume(),
                                c.getVolume(), c.getAccTradePrice(), c.getCreatedAt()))
                        .toList(), tradingProperties.getIndicators().getVolumeMa()),
                currentVolume,
                rsiTrend
        );
    }

    /**
     * 단순 이동평균 (SMA) 계산
     */
    public BigDecimal calculateMA(List<Candle> candles, int period) {
        if (candles.size() < period) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(candles.get(i).getTradePrice());
        }

        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    /**
     * RSI (Relative Strength Index) 계산
     */
    public BigDecimal calculateRSI(List<Candle> candles, int period) {
        if (candles.size() < period + 1) {
            return null;
        }

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // 첫 번째 평균 계산
        for (int i = 0; i < period; i++) {
            BigDecimal change = candles.get(i).getTradePrice()
                    .subtract(candles.get(i + 1).getTradePrice());

            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(
                        BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP));
    }

    /**
     * 스토캐스틱 %K 계산
     */
    public BigDecimal calculateStochasticK(List<Candle> candles, int period) {
        if (candles.size() < period) {
            return null;
        }

        BigDecimal currentClose = candles.get(0).getTradePrice();

        BigDecimal lowestLow = candles.stream()
                .limit(period)
                .map(Candle::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(currentClose);

        BigDecimal highestHigh = candles.stream()
                .limit(period)
                .map(Candle::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(currentClose);

        BigDecimal range = highestHigh.subtract(lowestLow);

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(50);
        }

        return currentClose.subtract(lowestLow)
                .divide(range, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 스토캐스틱 %D 계산 (Slow %K)
     */
    public BigDecimal calculateStochasticD(List<Candle> candles, int kPeriod, int dPeriod) {
        if (candles.size() < kPeriod + dPeriod) {
            return null;
        }

        BigDecimal sumK = BigDecimal.ZERO;
        int validCount = 0;

        for (int i = 0; i < dPeriod && i + kPeriod <= candles.size(); i++) {
            List<Candle> subList = candles.subList(i, candles.size());
            BigDecimal kValue = calculateStochasticK(subList, kPeriod);
            if (kValue != null) {
                sumK = sumK.add(kValue);
                validCount++;
            }
        }

        if (validCount == 0) {
            return null;
        }

        return sumK.divide(BigDecimal.valueOf(validCount), 8, RoundingMode.HALF_UP);
    }

    /**
     * RSI 추세 계산
     * 현재 RSI가 이전 RSI보다 상승 중이면 상승 추세
     */
    public int calculateRsiTrend(List<Candle> candles, int period) {
        if (candles.size() < period + 3) {
            return 0;
        }

        BigDecimal currentRsi = calculateRSI(candles, period);
        List<Candle> prevCandles = candles.subList(1, candles.size());
        BigDecimal prevRsi = calculateRSI(prevCandles, period);

        if (currentRsi == null || prevRsi == null) {
            return 0;
        }

        int comparison = currentRsi.compareTo(prevRsi);
        if (comparison > 0) {
            return 1;  // 상승 추세
        } else if (comparison < 0) {
            return -1; // 하락 추세
        }
        return 0;
    }

    /**
     * 거래량 이동평균 계산
     */
    public BigDecimal calculateVolumeMA(List<Candle> candles, int period) {
        if (candles.size() < period) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(candles.get(i).getVolume());
        }

        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    /**
     * 이전 캔들 기준 MA 계산 (크로스 이벤트 감지용)
     * @param market 마켓
     * @return 이전 캔들 기준 MA5, MA20 배열 [prevMa5, prevMa20], 계산 불가시 null
     */
    public BigDecimal[] calculatePreviousMAs(String market) {
        int requiredCandles = Math.max(tradingProperties.getIndicators().getMaLong(),
                tradingProperties.getIndicators().getRsiPeriod()) + 21; // 1개 더 필요
        List<Candle> candles = candleRepository.findByMarketOrderByDateTimeDesc(market, requiredCandles);

        if (candles.size() < 2) {
            return null;
        }

        // 이전 캔들 기준 (인덱스 1부터 시작하는 부분 리스트)
        List<Candle> prevCandles = candles.subList(1, candles.size());

        BigDecimal prevMa5 = calculateMA(prevCandles, tradingProperties.getIndicators().getMaShort());
        BigDecimal prevMa20 = calculateMA(prevCandles, tradingProperties.getIndicators().getMaMid());

        if (prevMa5 == null || prevMa20 == null) {
            return null;
        }

        return new BigDecimal[]{prevMa5, prevMa20};
    }

    /**
     * ATR (Average True Range) 계산
     * 변동성 측정 지표
     *
     * @param candles 캔들 리스트 (최신순 정렬)
     * @param period ATR 기간
     * @return ATR 값
     */
    public BigDecimal calculateATR(List<Candle> candles, int period) {
        if (candles.size() < period + 1) {
            return null;
        }

        BigDecimal sumTR = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            Candle current = candles.get(i);
            Candle prev = candles.get(i + 1);

            // True Range = max(High-Low, |High-PrevClose|, |Low-PrevClose|)
            BigDecimal highLow = current.getHighPrice().subtract(current.getLowPrice());
            BigDecimal highPrevClose = current.getHighPrice().subtract(prev.getTradePrice()).abs();
            BigDecimal lowPrevClose = current.getLowPrice().subtract(prev.getTradePrice()).abs();

            BigDecimal trueRange = highLow.max(highPrevClose).max(lowPrevClose);
            sumTR = sumTR.add(trueRange);
        }

        return sumTR.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    /**
     * ATR 퍼센트 계산 (가격 대비 변동성)
     * 포지션 사이징에 사용
     *
     * @param market 마켓
     * @return ATR% (예: 2.5 = 2.5% 변동성), 계산 불가시 null
     */
    public BigDecimal calculateATRPercent(String market) {
        int atrPeriod = tradingProperties.getIndicators().getAtrPeriod();
        List<Candle> candles = candleRepository.findByMarketOrderByDateTimeDesc(market, atrPeriod + 5);

        if (candles.isEmpty()) {
            return null;
        }

        BigDecimal atr = calculateATR(candles, atrPeriod);
        if (atr == null) {
            return null;
        }

        BigDecimal currentPrice = candles.get(0).getTradePrice();
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // ATR / 현재가 * 100 = ATR%
        return atr.divide(currentPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
