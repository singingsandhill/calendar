package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * 리스크 체크 및 청산 실행
     * 손절/익절/트레일링스탑 조건 충족 시 즉시 시장가 청산
     */
    @Transactional
    public CloseReason checkAndExecuteRiskRules(String market) {
        Optional<Position> openPositionOpt = positionRepository.findOpenPositionByMarket(market);

        if (openPositionOpt.isEmpty()) {
            return null;
        }

        Position position = openPositionOpt.get();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        if (currentPrice == null) {
            log.warn("Cannot get current price for risk check");
            return null;
        }

        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal pnlPct = position.calculateUnrealizedPnlPct(currentPriceBD);

        log.debug("Risk check - Entry: {}, Current: {}, PnL%: {}",
                position.getEntryPrice(), currentPriceBD, pnlPct);

        // 1. 손절 체크 (-8%)
        double stopLoss = tradingProperties.getRisk().getStopLoss();
        if (pnlPct.compareTo(BigDecimal.valueOf(stopLoss * 100)) <= 0) {
            log.warn("Stop-loss triggered! PnL: {}%", pnlPct);
            closePosition(position, currentPriceBD, CloseReason.STOP_LOSS);
            return CloseReason.STOP_LOSS;
        }

        // 2. High Water Mark 업데이트
        position.updateHighWaterMark(currentPriceBD);

        // 3. 트레일링 스탑 활성화 체크 (+10% 도달 시)
        double trailingActivation = tradingProperties.getRisk().getTrailingActivation();
        if (!position.isTrailingStopActive() &&
            pnlPct.compareTo(BigDecimal.valueOf(trailingActivation * 100)) >= 0) {

            // 트레일링 스탑 가격 설정 (-3% 추적)
            double trailingPct = tradingProperties.getRisk().getTrailingStop();
            BigDecimal trailingStopPrice = currentPriceBD.multiply(
                    BigDecimal.ONE.subtract(BigDecimal.valueOf(trailingPct)))
                    .setScale(0, RoundingMode.DOWN);
            position.activateTrailingStop(trailingStopPrice);
            log.info("Trailing stop activated at price: {} (current: {}, PnL: {}%)",
                    trailingStopPrice, currentPriceBD, pnlPct);
        }

        // 4. 트레일링 스탑 가격 업데이트 (상승만)
        if (position.isTrailingStopActive()) {
            double trailingPct = tradingProperties.getRisk().getTrailingStop();
            BigDecimal newTrailingStop = position.getHighWaterMark().multiply(
                    BigDecimal.ONE.subtract(BigDecimal.valueOf(trailingPct)))
                    .setScale(0, RoundingMode.DOWN);
            position.updateTrailingStop(newTrailingStop);

            // 5. 트레일링 스탑 트리거 체크
            if (position.shouldTrailingStop(currentPriceBD)) {
                log.info("Trailing stop triggered! Price: {}, Stop: {}, PnL: {}%",
                        currentPriceBD, position.getTrailingStopPrice(), pnlPct);
                closePosition(position, currentPriceBD, CloseReason.TRAILING_STOP);
                return CloseReason.TRAILING_STOP;
            }
        }

        // 6. 익절 체크 (+15%)
        double takeProfit = tradingProperties.getRisk().getTakeProfit();
        if (pnlPct.compareTo(BigDecimal.valueOf(takeProfit * 100)) >= 0) {
            log.info("Take-profit triggered! PnL: {}%", pnlPct);
            closePosition(position, currentPriceBD, CloseReason.TAKE_PROFIT);
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

                // 포지션 상태 업데이트
                position.close(exitPrice, position.getEntryVolume(), reason);
                positionRepository.save(position);

                log.info("Position closed - Entry: {}, Exit: {}, PnL: {} ({}%)",
                        position.getEntryPrice(), exitPrice,
                        position.getRealizedPnl(), position.getRealizedPnlPct());
            }
        } catch (Exception e) {
            log.error("Failed to close position", e);
        }
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

        Optional<Position> openPositionOpt = positionRepository.findOpenPositionByMarket(market);

        if (openPositionOpt.isEmpty()) {
            log.info("No open position to close");
            return;
        }

        Position position = openPositionOpt.get();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        if (currentPrice != null) {
            closePosition(position, BigDecimal.valueOf(currentPrice), CloseReason.MANUAL);
        }

        // 대기 중인 주문도 모두 취소
        bithumbApiClient.cancelAllPendingOrders();
    }
}
