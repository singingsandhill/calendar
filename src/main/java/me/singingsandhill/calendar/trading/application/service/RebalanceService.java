package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
public class RebalanceService {

    private static final Logger log = LoggerFactory.getLogger(RebalanceService.class);

    private final BithumbApiClient bithumbApiClient;
    private final IndicatorService indicatorService;
    private final TradingProperties tradingProperties;

    public RebalanceService(BithumbApiClient bithumbApiClient,
                            IndicatorService indicatorService,
                            TradingProperties tradingProperties) {
        this.bithumbApiClient = bithumbApiClient;
        this.indicatorService = indicatorService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 리밸런싱 필요 여부 확인 및 실행
     */
    @Transactional
    public RebalanceResult checkAndExecute(String market) {
        if (!tradingProperties.getRebalancing().isEnabled()) {
            return new RebalanceResult(false, null, null, null);
        }

        // 현재 잔고 조회
        BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
        BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        if (krwAccount == null || coinAccount == null || currentPrice == null) {
            log.warn("Cannot get account info for rebalancing");
            return new RebalanceResult(false, null, null, null);
        }

        BigDecimal krwBalance = new BigDecimal(krwAccount.balance());
        BigDecimal coinBalance = new BigDecimal(coinAccount.balance());
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);

        // 현재 비중 계산
        BigDecimal coinValue = coinBalance.multiply(currentPriceBD);
        BigDecimal totalValue = krwBalance.add(coinValue);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new RebalanceResult(false, null, null, null);
        }

        BigDecimal currentRatio = coinValue.divide(totalValue, 4, RoundingMode.HALF_UP);

        // 목표 비중 결정 (시장 상황에 따라)
        BigDecimal targetRatio = determineTargetRatio(market);

        // 편차 확인
        BigDecimal deviation = currentRatio.subtract(targetRatio).abs();
        double deviationTrigger = tradingProperties.getRebalancing().getDeviationTrigger();

        log.debug("Rebalance check - Current ratio: {}, Target: {}, Deviation: {}",
                currentRatio, targetRatio, deviation);

        if (deviation.compareTo(BigDecimal.valueOf(deviationTrigger)) < 0) {
            return new RebalanceResult(false, currentRatio, targetRatio, deviation);
        }

        // 리밸런싱 실행
        return executeRebalance(currentRatio, targetRatio, totalValue, currentPriceBD, coinBalance);
    }

    /**
     * 목표 비중 결정
     * 강세장 (현재가 > MA60): 70%
     * 약세장 (현재가 < MA60): 30%
     * 그 외: 50%
     */
    private BigDecimal determineTargetRatio(String market) {
        IndicatorResult indicators = indicatorService.calculate(market);

        if (indicators == null || indicators.ma60() == null) {
            return BigDecimal.valueOf(tradingProperties.getRebalancing().getDefaultRatio());
        }

        if (indicators.isPriceAboveMa60()) {
            return BigDecimal.valueOf(tradingProperties.getRebalancing().getBullRatio());
        } else if (indicators.isPriceBelowMa60()) {
            return BigDecimal.valueOf(tradingProperties.getRebalancing().getBearRatio());
        }

        return BigDecimal.valueOf(tradingProperties.getRebalancing().getDefaultRatio());
    }

    /**
     * 리밸런싱 실행
     */
    private RebalanceResult executeRebalance(BigDecimal currentRatio, BigDecimal targetRatio,
                                              BigDecimal totalValue, BigDecimal currentPrice,
                                              BigDecimal coinBalance) {
        BigDecimal targetCoinValue = totalValue.multiply(targetRatio);
        BigDecimal currentCoinValue = coinBalance.multiply(currentPrice);
        BigDecimal difference = targetCoinValue.subtract(currentCoinValue);

        log.info("Rebalancing - Current: {}%, Target: {}%, Difference: {} KRW",
                currentRatio.multiply(BigDecimal.valueOf(100)),
                targetRatio.multiply(BigDecimal.valueOf(100)),
                difference);

        try {
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // 코인 비중 부족 → 매수
                log.info("Buying {} KRW worth of coins", difference);
                BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(difference.abs());
                if (response != null) {
                    log.info("Rebalance buy order placed: {}", response.uuid());
                }
            } else {
                // 코인 비중 과다 → 매도
                BigDecimal sellVolume = difference.abs().divide(currentPrice, 8, RoundingMode.DOWN);
                log.info("Selling {} coins", sellVolume);
                BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(sellVolume);
                if (response != null) {
                    log.info("Rebalance sell order placed: {}", response.uuid());
                }
            }

            return new RebalanceResult(true, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        } catch (Exception e) {
            log.error("Failed to execute rebalance", e);
            return new RebalanceResult(false, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        }
    }

    public record RebalanceResult(
        boolean executed,
        BigDecimal currentRatio,
        BigDecimal targetRatio,
        BigDecimal deviation
    ) {}
}
