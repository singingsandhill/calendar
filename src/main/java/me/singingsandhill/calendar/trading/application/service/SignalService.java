package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.DivergenceResult;
import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class SignalService {

    private static final Logger log = LoggerFactory.getLogger(SignalService.class);

    private final SignalRepository signalRepository;
    private final IndicatorService indicatorService;
    private final DivergenceService divergenceService;
    private final TradingProperties tradingProperties;

    public SignalService(SignalRepository signalRepository,
                         IndicatorService indicatorService,
                         DivergenceService divergenceService,
                         TradingProperties tradingProperties) {
        this.signalRepository = signalRepository;
        this.indicatorService = indicatorService;
        this.divergenceService = divergenceService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 신호 생성 및 저장
     */
    @Transactional
    public Signal generateSignal(String market) {
        IndicatorResult indicators = indicatorService.calculate(market);
        if (indicators == null) {
            log.warn("Cannot calculate indicators for {}", market);
            return null;
        }

        DivergenceResult divergence = divergenceService.detect(market);

        // 점수 계산
        int maCrossScore = calculateMaCrossScore(indicators);
        int maTrendScore = calculateMaTrendScore(indicators);
        int rsiDivergenceScore = calculateRsiDivergenceScore(divergence);
        int rsiLevelScore = calculateRsiLevelScore(indicators);
        int stochDivergenceScore = calculateStochDivergenceScore(divergence);
        int stochLevelScore = calculateStochLevelScore(indicators);
        int volumeDivergenceScore = calculateVolumeDivergenceScore(divergence);
        int rsiTrendScore = calculateRsiTrendScore(indicators);

        int totalScore = maCrossScore + maTrendScore + rsiDivergenceScore + rsiLevelScore +
                stochDivergenceScore + stochLevelScore + volumeDivergenceScore + rsiTrendScore;

        // 신호 타입 결정
        SignalType signalType = determineSignalType(totalScore, divergence, indicators);

        Signal signal = new Signal(
                null, market, LocalDateTime.now(), signalType, totalScore,
                maCrossScore, maTrendScore, rsiDivergenceScore, rsiLevelScore,
                stochDivergenceScore, stochLevelScore, volumeDivergenceScore, rsiTrendScore,
                indicators.ma5(), indicators.ma20(), indicators.ma60(),
                indicators.rsi(), indicators.stochK(), indicators.stochD(),
                divergence.rsiDivergence(), divergence.stochDivergence(), divergence.volumeDivergence(),
                indicators.currentPrice(), false, LocalDateTime.now()
        );

        signalRepository.save(signal);
        log.info("Signal generated for {}: {} (score: {})", market, signalType, totalScore);

        return signal;
    }

    /**
     * MA 크로스오버 점수 계산
     * 골든크로스: +25, 데드크로스: -25
     */
    private int calculateMaCrossScore(IndicatorResult indicators) {
        if (indicators.ma5() == null || indicators.ma20() == null) {
            return 0;
        }

        if (indicators.isGoldenCross()) {
            return 25;
        } else if (indicators.isDeathCross()) {
            return -25;
        }
        return 0;
    }

    /**
     * MA 추세 점수 계산
     * 현재가 > MA60: +15, 현재가 < MA60: -15
     */
    private int calculateMaTrendScore(IndicatorResult indicators) {
        if (indicators.ma60() == null || indicators.currentPrice() == null) {
            return 0;
        }

        if (indicators.isPriceAboveMa60()) {
            return 15;
        } else if (indicators.isPriceBelowMa60()) {
            return -15;
        }
        return 0;
    }

    /**
     * RSI 다이버전스 점수 계산
     * 강세 다이버전스: +20, 약세 다이버전스: -20
     */
    private int calculateRsiDivergenceScore(DivergenceResult divergence) {
        if (divergence.rsiDivergence() == DivergenceType.BULLISH) {
            return 20;
        } else if (divergence.rsiDivergence() == DivergenceType.BEARISH) {
            return -20;
        }
        return 0;
    }

    /**
     * RSI 레벨 점수 계산
     * 과매도 (< 35): +15, 과매수 (> 65): -15
     */
    private int calculateRsiLevelScore(IndicatorResult indicators) {
        if (indicators.rsi() == null) {
            return 0;
        }

        int oversold = tradingProperties.getThresholds().getRsiOversold();
        int overbought = tradingProperties.getThresholds().getRsiOverbought();

        if (indicators.rsi().compareTo(BigDecimal.valueOf(oversold)) < 0) {
            return 15;
        } else if (indicators.rsi().compareTo(BigDecimal.valueOf(overbought)) > 0) {
            return -15;
        }
        return 0;
    }

    /**
     * 스토캐스틱 다이버전스 점수 계산
     * 강세 다이버전스: +15, 약세 다이버전스: -15
     */
    private int calculateStochDivergenceScore(DivergenceResult divergence) {
        if (divergence.stochDivergence() == DivergenceType.BULLISH) {
            return 15;
        } else if (divergence.stochDivergence() == DivergenceType.BEARISH) {
            return -15;
        }
        return 0;
    }

    /**
     * 스토캐스틱 레벨 점수 계산
     * 과매도 (< 25): +15, 과매수 (> 75): -15
     */
    private int calculateStochLevelScore(IndicatorResult indicators) {
        if (indicators.stochK() == null) {
            return 0;
        }

        int oversold = tradingProperties.getThresholds().getStochOversold();
        int overbought = tradingProperties.getThresholds().getStochOverbought();

        if (indicators.stochK().compareTo(BigDecimal.valueOf(oversold)) < 0) {
            return 15;
        } else if (indicators.stochK().compareTo(BigDecimal.valueOf(overbought)) > 0) {
            return -15;
        }
        return 0;
    }

    /**
     * 거래량 다이버전스 점수 계산
     * 강세 다이버전스: +20, 약세 다이버전스: -20
     */
    private int calculateVolumeDivergenceScore(DivergenceResult divergence) {
        if (divergence.volumeDivergence() == DivergenceType.BULLISH) {
            return 20;
        } else if (divergence.volumeDivergence() == DivergenceType.BEARISH) {
            return -20;
        }
        return 0;
    }

    /**
     * RSI 추세 점수 계산
     * RSI 상승 추세: +10, RSI 하락 추세: -10
     */
    private int calculateRsiTrendScore(IndicatorResult indicators) {
        if (indicators.isRsiUptrend()) {
            return 10;
        } else if (indicators.isRsiDowntrend()) {
            return -10;
        }
        return 0;
    }

    /**
     * 신호 타입 결정
     * 매수: 점수 >= 40 AND 현재가 > MA60 AND RSI < 70 AND StochK < 85
     * 매도: 점수 <= -40 AND 현재가 < MA60 AND RSI > 30 AND StochK > 15
     */
    private SignalType determineSignalType(int totalScore, DivergenceResult divergence, IndicatorResult indicators) {
        int buyThreshold = tradingProperties.getThresholds().getSignalBuy();
        int sellThreshold = tradingProperties.getThresholds().getSignalSell();
        int buyRsiMax = tradingProperties.getThresholds().getBuyRsiMax();
        int buyStochKMax = tradingProperties.getThresholds().getBuyStochKMax();
        int sellRsiMin = tradingProperties.getThresholds().getSellRsiMin();
        int sellStochKMin = tradingProperties.getThresholds().getSellStochKMin();

        // 매수 조건: score >= 40 AND price > MA60 AND RSI < 70 AND StochK < 85
        if (totalScore >= buyThreshold &&
            indicators.isPriceAboveMa60() &&
            indicators.rsi() != null && indicators.rsi().compareTo(BigDecimal.valueOf(buyRsiMax)) < 0 &&
            indicators.stochK() != null && indicators.stochK().compareTo(BigDecimal.valueOf(buyStochKMax)) < 0) {
            return SignalType.BUY;
        }

        // 매도 조건: score <= -40 AND price < MA60 AND RSI > 30 AND StochK > 15
        if (totalScore <= sellThreshold &&
            indicators.isPriceBelowMa60() &&
            indicators.rsi() != null && indicators.rsi().compareTo(BigDecimal.valueOf(sellRsiMin)) > 0 &&
            indicators.stochK() != null && indicators.stochK().compareTo(BigDecimal.valueOf(sellStochKMin)) > 0) {
            return SignalType.SELL;
        }

        return SignalType.HOLD;
    }
}
