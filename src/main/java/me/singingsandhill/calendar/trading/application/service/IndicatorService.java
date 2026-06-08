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

        // currentPrice 는 라이브 가격(index 0 tradePrice) 유지
        BigDecimal currentPrice = candles.get(0).getTradePrice();

        // P2-2: 형성 중(미완성) 현재봉 제외 — 지표는 종료봉만으로 계산 (스케줄러 5초 지연 의도와 정합).
        // 기본 OFF. Bithumb 이 index 0 을 형성봉으로 반환함을 확인한 뒤 ON 권장.
        List<Candle> indicatorCandles = candles;
        if (tradingProperties.getIndicators().isExcludeFormingCandle() && candles.size() > 1) {
            indicatorCandles = candles.subList(1, candles.size());
        }

        BigDecimal currentVolume = indicatorCandles.get(0).getVolume();
        int rsiTrend = calculateRsiTrend(indicatorCandles, tradingProperties.getIndicators().getRsiPeriod());

        return new IndicatorResult(
                currentPrice,
                calculateMA(indicatorCandles, tradingProperties.getIndicators().getMaShort()),
                calculateMA(indicatorCandles, tradingProperties.getIndicators().getMaMid()),
                calculateMA(indicatorCandles, tradingProperties.getIndicators().getMaLong()),
                calculateRSI(indicatorCandles, tradingProperties.getIndicators().getRsiPeriod()),
                // P2-5: 스코어링용 stochK = slow %K (fast %K 의 stochSlow SMA) — dead config 활용 + 1분봉 평활
                calculateStochasticD(indicatorCandles, tradingProperties.getIndicators().getStochK(),
                        tradingProperties.getIndicators().getStochSlow()),
                calculateStochasticD(indicatorCandles, tradingProperties.getIndicators().getStochK(),
                        tradingProperties.getIndicators().getStochD()),
                // P2-13: 거래량 MA 는 전용 메서드로 직접 계산 (Candle 생성자 악용 제거 — 동작 동일)
                calculateVolumeMA(indicatorCandles, tradingProperties.getIndicators().getVolumeMa()),
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

        // P2-4: Wilder 평활 RSI (표준). 캔들은 최신순(DESC) → 시간순 변화는 j = n-2(오래된) .. 0(최신).
        int n = candles.size();
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // 1) 시드: 가장 오래된 period 개의 변화 단순평균
        for (int j = n - 2; j >= n - 1 - period; j--) {
            BigDecimal change = candles.get(j).getTradePrice()
                    .subtract(candles.get(j + 1).getTradePrice());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }
        avgGain = avgGain.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        // 2) Wilder 평활: 나머지(더 최신) 변화에 대해 재귀 평활 avg = (avg*(p-1) + x)/p
        BigDecimal periodBD = BigDecimal.valueOf(period);
        BigDecimal periodMinus1 = BigDecimal.valueOf(period - 1);
        for (int j = n - 2 - period; j >= 0; j--) {
            BigDecimal change = candles.get(j).getTradePrice()
                    .subtract(candles.get(j + 1).getTradePrice());
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO;
            avgGain = avgGain.multiply(periodMinus1).add(gain).divide(periodBD, 8, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(periodMinus1).add(loss).divide(periodBD, 8, RoundingMode.HALF_UP);
        }

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
        // P2-6: 인접봉(1봉) 대신 lookback 봉 전 RSI 와 비교 + 최소 델타 요구 (1분봉 잡음 제거)
        int lookback = tradingProperties.getIndicators().getRsiTrendLookback();
        if (candles.size() < period + 1 + lookback) {
            return 0;
        }

        BigDecimal currentRsi = calculateRSI(candles, period);
        BigDecimal pastRsi = calculateRSI(candles.subList(lookback, candles.size()), period);

        if (currentRsi == null || pastRsi == null) {
            return 0;
        }

        return rsiTrend(currentRsi, pastRsi);
    }

    /**
     * P2-6: RSI 추세 판정 (순수 결정). |current − past| 가 minRsiTrendDelta 초과해야 추세 인정.
     */
    int rsiTrend(BigDecimal currentRsi, BigDecimal pastRsi) {
        BigDecimal minDelta = BigDecimal.valueOf(tradingProperties.getIndicators().getMinRsiTrendDelta());
        BigDecimal delta = currentRsi.subtract(pastRsi);
        if (delta.compareTo(minDelta) > 0) {
            return 1;
        }
        if (delta.compareTo(minDelta.negate()) < 0) {
            return -1;
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
