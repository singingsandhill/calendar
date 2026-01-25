package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RiskManagementService {

    private static final Logger log = LoggerFactory.getLogger(RiskManagementService.class);

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;

    public RiskManagementService(PositionRepository positionRepository,
                                  TradeRepository tradeRepository,
                                  BithumbApiClient bithumbApiClient,
                                  TradingProperties tradingProperties) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 리스크 체크 및 청산 실행 (다중 포지션 지원)
     * 손절/익절/트레일링스탑 조건 충족 시 즉시 시장가 청산
     * Issue #8: 각 포지션 처리 전 가격 갱신
     */
    @Transactional
    public CloseReason checkAndExecuteRiskRules(String market) {
        // 모든 열린 포지션 조회
        List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);

        if (openPositions.isEmpty()) {
            return null;
        }

        CloseReason lastCloseReason = null;

        // Issue #8: 각 포지션에 대해 리스크 체크 수행 (개별 가격 갱신)
        for (Position position : openPositions) {
            // Issue #5: 청산 시도 중인 포지션 스킵 (재시도 가능한 경우 제외)
            if (position.isClosingAttempted() && !position.shouldRetryClose()) {
                log.debug("Skipping position {} - closing attempted (count: {})",
                        position.getId(), position.getCloseAttemptCount());
                continue;
            }

            // Issue #8: 각 포지션 처리 전 가격 갱신
            Double currentPriceDouble = bithumbApiClient.getCurrentPrice();
            if (currentPriceDouble == null) {
                log.warn("Cannot get current price for risk check on position {}", position.getId());
                continue;
            }

            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceDouble);
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

        // 4. 트레일링 스탑 가격 업데이트 (상승만, 손익분기점 보장)
        if (position.isTrailingStopActive()) {
            double trailingPct = tradingProperties.getRisk().getTrailingStop();
            BigDecimal newTrailingStop = position.getHighWaterMark().multiply(
                    BigDecimal.ONE.subtract(BigDecimal.valueOf(trailingPct)))
                    .setScale(0, RoundingMode.DOWN);

            // 손익분기점 보장: 진입가 + 왕복 수수료(0.5%)
            double takerFeeRate = tradingProperties.getRisk().getTakerFeeRate();
            BigDecimal breakEvenPrice = position.getEntryPrice()
                    .multiply(BigDecimal.valueOf(1 + takerFeeRate * 2))
                    .setScale(0, RoundingMode.UP);

            // 트레일링 스탑이 손익분기점 아래로 내려가지 않도록
            if (newTrailingStop.compareTo(breakEvenPrice) < 0) {
                log.debug("Trailing stop adjusted to break-even: {} -> {} (entry: {})",
                        newTrailingStop, breakEvenPrice, position.getEntryPrice());
                newTrailingStop = breakEvenPrice;
            }

            position.updateTrailingStop(newTrailingStop);

            // 5. 트레일링 스탑 트리거 체크
            if (position.shouldTrailingStop(currentPrice)) {
                log.info("Trailing stop triggered for position {}! Price: {}, Stop: {} (breakEven: {}), PnL: {}%",
                        position.getId(), currentPrice, position.getTrailingStopPrice(), breakEvenPrice, pnlPct);
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
     * Issue #1: API 호출 먼저, 성공 시에만 Trade 저장
     * Issue #3: 체결가 재시도 로직
     * Issue #5: 실패 시 청산 시도 추적
     */
    @Transactional
    public void closePosition(Position position, BigDecimal exitPrice, CloseReason reason) {
        log.info("Closing position {} with reason: {}", position.getId(), reason);

        // Issue #5: 청산 시도 추적
        position.markClosingAttempted();

        try {
            // Issue #1: API 호출 먼저
            BithumbOrderResponse orderResponse = bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (orderResponse == null) {
                log.warn("Close order failed - null response for position {}", position.getId());
                // Issue #5: 청산 시도 상태 저장 (다음 사이클에서 재시도 가능)
                positionRepository.save(position);
                return;
            }

            log.info("Close order placed: {}", orderResponse.uuid());

            // 수수료 추출
            BigDecimal fee = extractFee(orderResponse);

            // Issue #3: 체결가 재시도 로직
            BigDecimal actualExitPrice = extractExecutedPriceWithRetry(orderResponse, 3);
            if (actualExitPrice == null) {
                actualExitPrice = exitPrice;
                log.warn("Using parameter exitPrice as final fallback: {}", actualExitPrice);
            }

            // Issue #1: 성공 시에만 Trade 저장
            String uuid = orderResponse.uuid() != null ? orderResponse.uuid() : UUID.randomUUID().toString();
            Trade trade = Trade.createSellOrder(
                    uuid,
                    position.getId(),
                    position.getMarket(),
                    actualExitPrice,
                    position.getEntryVolume(),
                    "market",
                    null,
                    "Risk management: " + reason.name()
            );
            trade.markExecuted(actualExitPrice, position.getEntryVolume(), fee);
            tradeRepository.save(trade);

            // 포지션 상태 업데이트 (실제 체결가 사용)
            position.close(actualExitPrice, position.getEntryVolume(), reason, fee);
            positionRepository.save(position);

            log.info("Position closed - Entry: {}, Exit: {} (expected: {}), PnL: {} ({}%), Fee: {}",
                    position.getEntryPrice(), actualExitPrice, exitPrice,
                    position.getRealizedPnl(), position.getRealizedPnlPct(), fee);

        } catch (Exception e) {
            log.error("Failed to close position {}", position.getId(), e);
            // Issue #5: 예외 발생 시에도 청산 시도 상태 저장
            positionRepository.save(position);
        }
    }

    /**
     * Issue #3: 체결가 재시도 로직
     * API 응답에서 체결가를 추출하고, 실패 시 주문 상세 조회 재시도
     */
    private BigDecimal extractExecutedPriceWithRetry(BithumbOrderResponse response, int maxRetries) {
        // 1차: API 응답에서 직접 추출
        BigDecimal price = extractExecutedPrice(response);
        if (price != null) {
            return price;
        }

        // 2차: 주문 상세 조회 재시도
        if (response.uuid() != null) {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(500L * (i + 1)); // 백오프: 500ms, 1000ms, 1500ms
                    BithumbOrderResponse orderDetail = bithumbApiClient.getOrder(response.uuid());
                    if (orderDetail != null) {
                        price = extractExecutedPrice(orderDetail);
                        if (price != null) {
                            log.debug("Extracted execution price on retry {}: {}", i + 1, price);
                            return price;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for execution price retry");
                    break;
                }
            }
        }

        // 3차: 현재가 조회
        Double currentPrice = bithumbApiClient.getCurrentPrice();
        if (currentPrice != null) {
            log.debug("Using current price as fallback for exit: {}", currentPrice);
            return BigDecimal.valueOf(currentPrice);
        }

        return null;
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
     * BithumbOrderResponse에서 실제 체결가 추출
     * trades 리스트 전체의 가중평균 가격 계산 (부분 체결 대응)
     */
    private BigDecimal extractExecutedPrice(BithumbOrderResponse response) {
        if (response.trades() != null && !response.trades().isEmpty()) {
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalVolume = BigDecimal.ZERO;

            for (BithumbOrderResponse.TradeDetail trade : response.trades()) {
                if (trade.price() != null && trade.volume() != null
                        && !trade.price().isEmpty() && !trade.volume().isEmpty()) {
                    try {
                        BigDecimal price = new BigDecimal(trade.price());
                        BigDecimal volume = new BigDecimal(trade.volume());
                        totalValue = totalValue.add(price.multiply(volume));
                        totalVolume = totalVolume.add(volume);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse trade: price={}, volume={}", trade.price(), trade.volume());
                    }
                }
            }

            if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgPrice = totalValue.divide(totalVolume, 8, RoundingMode.HALF_UP);
                log.debug("Calculated weighted average exit price: {} (from {} trades)", avgPrice, response.trades().size());
                return avgPrice;
            }
        }
        return null;
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
