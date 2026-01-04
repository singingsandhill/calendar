package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RebalanceService {

    private static final Logger log = LoggerFactory.getLogger(RebalanceService.class);

    private volatile Instant lastRebalanceTime = null;

    private final BithumbApiClient bithumbApiClient;
    private final IndicatorService indicatorService;
    private final TradingProperties tradingProperties;
    private final TradeRepository tradeRepository;

    public RebalanceService(BithumbApiClient bithumbApiClient,
                            IndicatorService indicatorService,
                            TradingProperties tradingProperties,
                            TradeRepository tradeRepository) {
        this.bithumbApiClient = bithumbApiClient;
        this.indicatorService = indicatorService;
        this.tradingProperties = tradingProperties;
        this.tradeRepository = tradeRepository;
    }

    /**
     * 리밸런싱 필요 여부 확인 및 실행
     */
    @Transactional
    public RebalanceResult checkAndExecute(String market) {
        if (!tradingProperties.getRebalancing().isEnabled()) {
            return new RebalanceResult(false, null, null, null);
        }

        // 쿨다운 체크
        if (!isCooldownElapsed()) {
            log.debug("Rebalance skipped - cooldown not elapsed. Last: {}", lastRebalanceTime);
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
        if (targetRatio == null) {
            // MA60 데이터 부족 시 스킵
            log.debug("Rebalance skipped - insufficient indicator data");
            return new RebalanceResult(false, currentRatio, null, null);
        }

        // 편차 확인
        BigDecimal deviation = currentRatio.subtract(targetRatio).abs();
        double deviationTrigger = tradingProperties.getRebalancing().getDeviationTrigger();

        log.debug("Rebalance check - Current ratio: {}, Target: {}, Deviation: {}",
                currentRatio, targetRatio, deviation);

        if (deviation.compareTo(BigDecimal.valueOf(deviationTrigger)) < 0) {
            return new RebalanceResult(false, currentRatio, targetRatio, deviation);
        }

        // 리밸런싱 실행
        return executeRebalance(currentRatio, targetRatio, totalValue, currentPriceBD, coinBalance, krwBalance);
    }

    /**
     * 쿨다운 경과 여부 확인
     */
    private boolean isCooldownElapsed() {
        if (lastRebalanceTime == null) {
            return true;
        }
        long cooldownMinutes = tradingProperties.getRebalancing().getCooldownMinutes();
        Duration elapsed = Duration.between(lastRebalanceTime, Instant.now());
        return elapsed.toMinutes() >= cooldownMinutes;
    }

    /**
     * 목표 비중 결정
     * 강세장 (현재가 > MA60): 70%
     * 약세장 (현재가 < MA60): 30%
     * 그 외: 50%
     * @return 목표 비중. MA60 데이터 부족 시 skipWhenDataInsufficient=true이면 null 반환
     */
    private BigDecimal determineTargetRatio(String market) {
        IndicatorResult indicators = indicatorService.calculate(market);

        if (indicators == null || indicators.ma60() == null) {
            if (tradingProperties.getRebalancing().isSkipWhenDataInsufficient()) {
                log.warn("MA60 data insufficient for rebalancing - skipping");
                return null;
            }
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
                                              BigDecimal coinBalance, BigDecimal krwBalance) {
        BigDecimal targetCoinValue = totalValue.multiply(targetRatio);
        BigDecimal currentCoinValue = coinBalance.multiply(currentPrice);
        BigDecimal difference = targetCoinValue.subtract(currentCoinValue);
        String market = tradingProperties.getBot().getMarket();

        // 최소 주문 금액 검증
        BigDecimal minOrderAmount = BigDecimal.valueOf(tradingProperties.getRebalancing().getMinOrderAmount());
        if (difference.abs().compareTo(minOrderAmount) < 0) {
            log.debug("Rebalance amount too small: {} KRW (min: {})", difference.abs(), minOrderAmount);
            return new RebalanceResult(false, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        }

        // 슬리피지 버퍼 적용
        double slippageBuffer = tradingProperties.getRebalancing().getSlippageBuffer();
        BigDecimal adjustedDifference = difference.multiply(BigDecimal.valueOf(1.0 - slippageBuffer));

        log.info("Rebalancing - Current: {}%, Target: {}%, Difference: {} KRW (adjusted: {} KRW)",
                currentRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                targetRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                difference.setScale(0, RoundingMode.HALF_UP),
                adjustedDifference.setScale(0, RoundingMode.HALF_UP));
        log.info("Before rebalance - KRW: {}, Coin: {}, CoinValue: {} KRW",
                krwBalance.setScale(0, RoundingMode.HALF_UP),
                coinBalance.setScale(8, RoundingMode.DOWN),
                currentCoinValue.setScale(0, RoundingMode.HALF_UP));

        try {
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // 코인 비중 부족 → 매수
                // Issue #1: API 호출 먼저
                BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(adjustedDifference.abs());
                if (response != null) {
                    log.info("Rebalance buy order placed: uuid={}", response.uuid());
                    BigDecimal fee = extractFee(response);
                    BigDecimal volume = adjustedDifference.abs().divide(currentPrice, 8, RoundingMode.DOWN);

                    // Issue #1: 성공 시에만 Trade 저장
                    String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
                    Trade trade = Trade.createBuyOrder(
                            uuid, market, currentPrice, adjustedDifference.abs(),
                            "market", null, "Rebalancing buy"
                    );
                    trade.markExecuted(currentPrice, volume, fee);
                    tradeRepository.save(trade);
                    log.info("Rebalance buy completed - Volume: {}, Fee: {} KRW", volume, fee);
                } else {
                    log.warn("Rebalance buy failed - no response from API");
                    return new RebalanceResult(false, currentRatio, targetRatio,
                            currentRatio.subtract(targetRatio).abs());
                }
            } else {
                // 코인 비중 과다 → 매도
                BigDecimal sellVolume = adjustedDifference.abs().divide(currentPrice, 8, RoundingMode.DOWN);
                // Issue #1: API 호출 먼저
                BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(sellVolume);
                if (response != null) {
                    log.info("Rebalance sell order placed: uuid={}", response.uuid());
                    BigDecimal fee = extractFee(response);

                    // Issue #1: 성공 시에만 Trade 저장
                    String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
                    Trade trade = Trade.createSellOrder(
                            uuid, null, market, currentPrice, sellVolume,
                            "market", null, "Rebalancing sell"
                    );
                    trade.markExecuted(currentPrice, sellVolume, fee);
                    tradeRepository.save(trade);
                    log.info("Rebalance sell completed - Volume: {}, Fee: {} KRW", sellVolume, fee);
                } else {
                    log.warn("Rebalance sell failed - no response from API");
                    return new RebalanceResult(false, currentRatio, targetRatio,
                            currentRatio.subtract(targetRatio).abs());
                }
            }

            // 쿨다운 시간 갱신
            lastRebalanceTime = Instant.now();
            log.info("Rebalance executed successfully. Next rebalance available after {} minutes",
                    tradingProperties.getRebalancing().getCooldownMinutes());

            return new RebalanceResult(true, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        } catch (Exception e) {
            // Issue #1: Trade는 API 성공 후에만 저장하므로 별도의 상태 업데이트 불필요
            log.error("Failed to execute rebalance: {}", e.getMessage(), e);
            return new RebalanceResult(false, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        }
    }

    /**
     * BithumbOrderResponse에서 수수료 추출
     */
    private BigDecimal extractFee(BithumbOrderResponse response) {
        if (response.paidFee() != null && !response.paidFee().isEmpty()) {
            try {
                return new BigDecimal(response.paidFee());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse fee: {}", response.paidFee());
            }
        }
        return BigDecimal.ZERO;
    }

    public record RebalanceResult(
        boolean executed,
        BigDecimal currentRatio,
        BigDecimal targetRatio,
        BigDecimal deviation
    ) {}
}
