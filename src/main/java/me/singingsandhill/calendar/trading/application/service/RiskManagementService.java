package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RiskManagementService {

    private static final Logger log = LoggerFactory.getLogger(RiskManagementService.class);

    private final PositionRepository positionRepository;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;

    public RiskManagementService(PositionRepository positionRepository,
                                  BithumbApiClient bithumbApiClient,
                                  TradingProperties tradingProperties) {
        this.positionRepository = positionRepository;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 리스크 체크 및 청산 실행 (다중 포지션 지원)
     * 손절/익절/트레일링스탑 조건 충족 시 즉시 시장가 청산
     */
    @Transactional
    public CloseReason checkAndExecuteRiskRules(String market) {
        // 모든 열린 포지션 조회
        List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);

        if (openPositions.isEmpty()) {
            return null;
        }

        Double currentPriceDouble = bithumbApiClient.getCurrentPrice();
        if (currentPriceDouble == null) {
            log.warn("Cannot get current price for risk check");
            return null;
        }

        BigDecimal currentPrice = BigDecimal.valueOf(currentPriceDouble);
        CloseReason lastCloseReason = null;

        // 각 포지션에 대해 리스크 체크 수행
        for (Position position : openPositions) {
            CloseReason reason = checkPositionRisk(position, currentPrice);
            if (reason != null) {
                lastCloseReason = reason;
            }
        }

        return lastCloseReason;
    }

    /**
     * 단일 포지션 리스크 체크
     * 수수료를 포함한 정확한 손익률로 리스크 판단
     */
    private CloseReason checkPositionRisk(Position position, BigDecimal currentPrice) {
        // 수수료를 포함한 정확한 손익률 계산
        BigDecimal feeRate = BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate());
        BigDecimal pnlPct = position.calculateUnrealizedPnlPctWithFee(currentPrice, feeRate);

        log.debug("Risk check - Position {}: Entry={}, Current={}, PnL%={} (fee-adjusted)",
                position.getId(), position.getEntryPrice(), currentPrice, pnlPct);

        // 1. 손절 체크 (-8%)
        double stopLoss = tradingProperties.getRisk().getStopLoss();
        if (pnlPct.compareTo(BigDecimal.valueOf(stopLoss * 100)) <= 0) {
            log.warn("Stop-loss triggered for position {}! PnL: {}%", position.getId(), pnlPct);
            closePosition(position, currentPrice, CloseReason.STOP_LOSS);
            return CloseReason.STOP_LOSS;
        }

        // 2. High Water Mark 업데이트
        position.updateHighWaterMark(currentPrice);

        // 3. 트레일링 스탑 활성화 체크 (+10% 도달 시)
        double trailingActivation = tradingProperties.getRisk().getTrailingActivation();
        if (!position.isTrailingStopActive() &&
            pnlPct.compareTo(BigDecimal.valueOf(trailingActivation * 100)) >= 0) {

            // 트레일링 스탑 가격 설정 (-3% 추적)
            double trailingPct = tradingProperties.getRisk().getTrailingStop();
            BigDecimal trailingStopPrice = currentPrice.multiply(
                    BigDecimal.ONE.subtract(BigDecimal.valueOf(trailingPct)))
                    .setScale(0, RoundingMode.DOWN);
            position.activateTrailingStop(trailingStopPrice);
            log.info("Trailing stop activated for position {}: {} (current: {}, PnL: {}%)",
                    position.getId(), trailingStopPrice, currentPrice, pnlPct);
        }

        // 4. 트레일링 스탑 가격 업데이트 (상승만)
        if (position.isTrailingStopActive()) {
            double trailingPct = tradingProperties.getRisk().getTrailingStop();
            BigDecimal newTrailingStop = position.getHighWaterMark().multiply(
                    BigDecimal.ONE.subtract(BigDecimal.valueOf(trailingPct)))
                    .setScale(0, RoundingMode.DOWN);
            position.updateTrailingStop(newTrailingStop);

            // 5. 트레일링 스탑 트리거 체크
            if (position.shouldTrailingStop(currentPrice)) {
                log.info("Trailing stop triggered for position {}! Price: {}, Stop: {}, PnL: {}%",
                        position.getId(), currentPrice, position.getTrailingStopPrice(), pnlPct);
                closePosition(position, currentPrice, CloseReason.TRAILING_STOP);
                return CloseReason.TRAILING_STOP;
            }
        }

        // 6. 익절 체크 (+15%)
        double takeProfit = tradingProperties.getRisk().getTakeProfit();
        if (pnlPct.compareTo(BigDecimal.valueOf(takeProfit * 100)) >= 0) {
            log.info("Take-profit triggered for position {}! PnL: {}%", position.getId(), pnlPct);
            closePosition(position, currentPrice, CloseReason.TAKE_PROFIT);
            return CloseReason.TAKE_PROFIT;
        }

        // 7. Position 저장 (trailing stop 상태 유지)
        positionRepository.save(position);

        return null;
    }

    /**
     * 포지션 청산 실행
     */
    @Transactional
    public void closePosition(Position position, BigDecimal exitPrice, CloseReason reason) {
        log.info("Closing position {} with reason: {}", position.getId(), reason);

        try {
            // 시장가 매도 주문 실행
            BithumbOrderResponse orderResponse = bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (orderResponse != null) {
                log.info("Close order placed: {}", orderResponse.uuid());

                // 수수료 추출
                BigDecimal fee = extractFee(orderResponse);

                // 포지션 상태 업데이트 (수수료 포함)
                position.close(exitPrice, position.getEntryVolume(), reason, fee);
                positionRepository.save(position);

                log.info("Position closed - Entry: {}, Exit: {}, PnL: {} ({}%), Fee: {}",
                        position.getEntryPrice(), exitPrice,
                        position.getRealizedPnl(), position.getRealizedPnlPct(), fee);
            }
        } catch (Exception e) {
            log.error("Failed to close position", e);
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

    /**
     * 손절가 계산
     */
    public BigDecimal calculateStopLossPrice(BigDecimal entryPrice) {
        double stopLoss = tradingProperties.getRisk().getStopLoss();
        return entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(stopLoss)))
                .setScale(0, RoundingMode.DOWN);
    }

    /**
     * 익절가 계산
     */
    public BigDecimal calculateTakeProfitPrice(BigDecimal entryPrice) {
        double takeProfit = tradingProperties.getRisk().getTakeProfit();
        return entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(takeProfit)))
                .setScale(0, RoundingMode.UP);
    }

    /**
     * 긴급 청산 (모든 포지션)
     */
    @Transactional
    public void emergencyClose(String market) {
        log.warn("Emergency close triggered for {}", market);

        // 모든 열린 포지션 조회
        List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);

        if (openPositions.isEmpty()) {
            log.info("No open positions to close");
            return;
        }

        Double currentPriceDouble = bithumbApiClient.getCurrentPrice();
        if (currentPriceDouble == null) {
            log.error("Cannot get current price for emergency close");
            return;
        }

        BigDecimal currentPrice = BigDecimal.valueOf(currentPriceDouble);

        // 모든 포지션 청산
        for (Position position : openPositions) {
            log.warn("Emergency closing position {}", position.getId());
            closePosition(position, currentPrice, CloseReason.MANUAL);
        }

        // 대기 중인 주문도 모두 취소
        bithumbApiClient.cancelAllPendingOrders();
    }
}
