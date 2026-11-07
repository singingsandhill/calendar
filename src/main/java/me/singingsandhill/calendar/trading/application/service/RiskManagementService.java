package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.ExitThresholds;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.trade.BithumbTickSize;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class RiskManagementService {

    private static final Logger log = LoggerFactory.getLogger(RiskManagementService.class);

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;
    private final TradingEventService tradingEventService;
    private final TradingCircuitBreaker circuitBreaker;
    private final IndicatorService indicatorService;   // ATR 손절 모드 (ADR strategy/0011)
    private final SignalRepository signalRepository;   // SIGNAL_GATED 시간청산
    // P0-3b: 영속화만 짧은 트랜잭션. 주문 HTTP/sleep 은 트랜잭션 밖.
    private final TransactionTemplate txTemplate;

    public RiskManagementService(PositionRepository positionRepository,
                                  TradeRepository tradeRepository,
                                  BithumbApiClient bithumbApiClient,
                                  TradingProperties tradingProperties,
                                  TradingEventService tradingEventService,
                                  TradingCircuitBreaker circuitBreaker,
                                  PlatformTransactionManager transactionManager,
                                  IndicatorService indicatorService,
                                  SignalRepository signalRepository) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
        this.tradingEventService = tradingEventService;
        this.circuitBreaker = circuitBreaker;
        this.indicatorService = indicatorService;
        this.signalRepository = signalRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 리스크 체크 및 청산 실행 (다중 포지션 지원)
     * 손절/익절/트레일링스탑 조건 충족 시 즉시 시장가 청산
     * Issue #8: 각 포지션 처리 전 가격 갱신
     */
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
                if (position.getCloseAttemptCount() >= 3) {
                    log.error("Position {} stuck in closing state (count: {}), waiting for backoff. Manual intervention may be required.",
                            position.getId(), position.getCloseAttemptCount());
                } else {
                    log.debug("Skipping position {} - closing attempted (count: {})",
                            position.getId(), position.getCloseAttemptCount());
                }
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

        // 0. 보유 중 최저가(MAE) — 손절 판정 *전에* 갱신한다. 손절이 걸리는 틱의 가격이 곧 최저가라 판정 뒤에 두면
        //    손절된 포지션의 MAE 가 체계적으로 빠진다 (ADR trading/observability/0004). 저장은 아래 청산·8단계 저장이 한다.
        position.updateLowWaterMark(currentPrice);

        // 리밸런싱 포지션 (ADR trading/risk/0007, 4-C) — 엔진이 켜져 있으면 장중 SL/TP/트레일링/시간청산 대상이 아니고
        // 재난 대비 비상 손절만 받는다(자산 30~70% 를 -1.5% 손절에 태우던 손절 루프 차단). 엔진이 꺼져 있으면 그 포지션을
        // 관리할 주체가 없으므로 아래 일반 출구를 그대로 받는다.
        if (position.getEntrySource() == EntrySource.REBALANCE && tradingProperties.getRebalancing().isEnabled()) {
            position.updateHighWaterMark(currentPrice);
            double catastrophic = tradingProperties.getRebalancing().getCatastrophicStopPct();
            if (pnlPct.compareTo(BigDecimal.valueOf(catastrophic * 100)) <= 0) {
                log.warn("Catastrophic stop triggered for rebalance position {}! PnL: {}%", position.getId(), pnlPct);
                closePosition(position, currentPrice, CloseReason.CATASTROPHIC_STOP);
                return CloseReason.CATASTROPHIC_STOP;
            }
            positionRepository.save(position);
            return null;
        }

        // 1. 손절 체크 (stop-loss, 기본 -1.5% — P1-1). 유효 손절 = min(손절, -min-stop-ticks 틱/진입가) — 290원이면
        //    -1.5%(4.4틱) → -1.72%(5틱). stop-mode=ATR 이면 손절 = -atr-multiplier × 집계봉 ATR% (ADR strategy/0011)
        double stopLoss = effectiveStopLoss(position.getMarket(), position.getEntryPrice());
        if (pnlPct.compareTo(BigDecimal.valueOf(stopLoss * 100)) <= 0) {
            log.warn("Stop-loss triggered for position {}! PnL: {}% (stop: {}%)", position.getId(), pnlPct,
                    BigDecimal.valueOf(stopLoss * 100).setScale(3, RoundingMode.HALF_UP));
            closePosition(position, currentPrice, CloseReason.STOP_LOSS);
            return CloseReason.STOP_LOSS;
        }

        // 2. High Water Mark 업데이트
        position.updateHighWaterMark(currentPrice);

        // 3. 트레일링 스탑 활성화 체크 (trailing-activation, 기본 +1.5% 도달 시 — P1-2).
        //    time-exit-mode=TIGHTEN_TRAIL 이면 maxHold 를 넘긴 이익 포지션도 시간청산 대신 여기서 활성화 (ADR strategy/0011)
        double trailingActivation = tradingProperties.getRisk().getTrailingActivation();
        boolean tightenOnStale = tradingProperties.getBot().getTimeExitMode() == TradingProperties.Bot.TimeExitMode.TIGHTEN_TRAIL
                && pnlPct.signum() >= 0 && isStale(position);
        if (!position.isTrailingStopActive() &&
            (pnlPct.compareTo(BigDecimal.valueOf(trailingActivation * 100)) >= 0 || tightenOnStale)) {

            // 트레일링 스탑 가격 설정 (trailing-stop, 기본 -0.8% 추적 — P1-2, 폭은 최소 min-trailing-ticks 틱)
            BigDecimal trailingStopPrice = trailingStopPrice(currentPrice);
            position.activateTrailingStop(trailingStopPrice);
            log.info("Trailing stop activated for position {}: {} (current: {}, PnL: {}%{})",
                    position.getId(), trailingStopPrice, currentPrice, pnlPct,
                    tightenOnStale ? ", maxHold 경과 TIGHTEN_TRAIL" : "");
        }

        // 4. 트레일링 스탑 가격 업데이트 (상승만, 손익분기점 보장)
        if (position.isTrailingStopActive()) {
            BigDecimal newTrailingStop = trailingStopPrice(position.getHighWaterMark());

            // 손익분기점 보장: 진입가 + 왕복 수수료(0.5%), 호가 격자로 올림
            double takerFeeRate = tradingProperties.getRisk().getTakerFeeRate();
            BigDecimal breakEvenPrice = BithumbTickSize.roundUp(position.getEntryPrice()
                    .multiply(BigDecimal.valueOf(1 + takerFeeRate * 2)));

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

        // 6. 익절 체크 (take-profit, 기본 +3% — P1-2)
        double takeProfit = tradingProperties.getRisk().getTakeProfit();
        if (pnlPct.compareTo(BigDecimal.valueOf(takeProfit * 100)) >= 0) {
            log.info("Take-profit triggered for position {}! PnL: {}%", position.getId(), pnlPct);
            closePosition(position, currentPrice, CloseReason.TAKE_PROFIT);
            return CloseReason.TAKE_PROFIT;
        }

        // 7. P2-8: 정체 포지션 시간 청산 (maxHold 초과 + 손익분기 이상 → 자본 회수, 모드별 — ADR strategy/0011)
        if (shouldTimeExit(position, pnlPct)) {
            log.info("Time-exit triggered for position {} (stale > maxHold, PnL: {}%)", position.getId(), pnlPct);
            closePosition(position, currentPrice, CloseReason.TIME_EXIT);
            return CloseReason.TIME_EXIT;
        }

        // 8. Position 저장 (trailing stop 상태 유지)
        positionRepository.save(position);

        return null;
    }

    /**
     * 포지션 청산 실행
     * Issue #1: API 호출 먼저, 성공 시에만 Trade 저장
     * Issue #3: 체결가 재시도 로직
     * Issue #5: 실패 시 청산 시도 추적
     */
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

            // 포지션 상태 업데이트 (실제 체결가 사용)
            position.close(actualExitPrice, position.getEntryVolume(), reason, fee);

            // P0-3b: 영속화만 짧은 트랜잭션 (주문 HTTP/sleep 은 위에서 완료)
            txTemplate.executeWithoutResult(status -> {
                tradeRepository.save(trade);
                positionRepository.save(position);
            });

            // P0-2: 서킷브레이커 연속 손실 스트릭 갱신
            circuitBreaker.recordOutcome(position.getRealizedPnl());

            log.info("Position closed - Entry: {}, Exit: {} (expected: {}), PnL: {} ({}%), Fee: {}",
                    position.getEntryPrice(), actualExitPrice, exitPrice,
                    position.getRealizedPnl(), position.getRealizedPnlPct(), fee);

            BigDecimal pnlPct = position.getRealizedPnlPct();
            TradingEventLevel level = switch (reason) {
                case TAKE_PROFIT -> TradingEventLevel.OK;
                case TRAILING_STOP -> TradingEventLevel.NOTICE;
                case STOP_LOSS -> TradingEventLevel.WARNING;
                case CATASTROPHIC_STOP -> TradingEventLevel.CRITICAL;
                default -> TradingEventLevel.NOTICE;
            };
            tradingEventService.record(level, "POSITION_CLOSED_" + reason.name(),
                    position.getMarket(),
                    String.format("포지션 #%d 청산 (%s) — 가격 %s, 손익률 %s%%",
                            position.getId(), reason.name(),
                            actualExitPrice.toPlainString(),
                            pnlPct != null ? pnlPct.toPlainString() : "-"));

        } catch (Exception e) {
            log.error("Failed to close position {}", position.getId(), e);
            // Issue #5: 예외 발생 시에도 청산 시도 상태 저장
            positionRepository.save(position);
            tradingEventService.record(TradingEventLevel.CRITICAL, "POSITION_CLOSE_FAILED",
                    position.getMarket(),
                    String.format("포지션 #%d 청산 실패 (사유 %s, 시도 %d회): %s",
                            position.getId(), reason.name(),
                            position.getCloseAttemptCount(),
                            e.getClass().getSimpleName() + " " + e.getMessage()));
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
     * P2-8: 정체 포지션 시간 청산 판정 (모드별 — ADR trading/strategy/0011).
     * maxHoldMinutes 초과 + 수수료차감 PnL ≥ 0%(손익분기) 이면 time-exit-mode 에 따라:
     * BREAKEVEN_CLOSE(기본) 청산 / TIGHTEN_TRAIL 청산 안 함(3단계에서 트레일링 활성화) / SIGNAL_GATED 최신 신호 점수 ≤ 0 일 때만.
     * 적자 포지션은 손절이 처리 — time-exit-loss-threshold(기본 null)가 있을 때만 손익률 ≥ 그 값인 작은 손실을 청산.
     */
    boolean shouldTimeExit(Position position, BigDecimal pnlPct) {
        if (pnlPct == null || !isStale(position)) {
            return false;
        }
        if (pnlPct.signum() < 0) {
            Double lossThreshold = tradingProperties.getBot().getTimeExitLossThreshold();
            return lossThreshold != null && pnlPct.compareTo(BigDecimal.valueOf(lossThreshold * 100)) >= 0;
        }
        return switch (tradingProperties.getBot().getTimeExitMode()) {
            case BREAKEVEN_CLOSE -> true;
            case TIGHTEN_TRAIL -> false;
            // 신호가 없으면 판정할 근거가 없으므로 현행(BREAKEVEN_CLOSE)처럼 청산
            case SIGNAL_GATED -> signalRepository.findLatestByMarket(position.getMarket())
                    .map(signal -> signal.getTotalScore() <= 0)
                    .orElse(true);
        };
    }

    /** maxHoldMinutes 경과 여부 (0 = 시간청산 비활성). */
    private boolean isStale(Position position) {
        long maxHold = tradingProperties.getBot().getMaxHoldMinutes();
        if (maxHold <= 0 || position.getOpenedAt() == null) {
            return false;
        }
        return ChronoUnit.MINUTES.between(position.getOpenedAt(), LocalDateTime.now()) >= maxHold;
    }

    /** 트레일링 스탑 가격 = 기준가 - max(기준가 × trailing-stop, min-trailing-ticks 틱), 호가 격자로 내림. */
    private BigDecimal trailingStopPrice(BigDecimal reference) {
        TradingProperties.Risk risk = tradingProperties.getRisk();
        return ExitThresholds.trailingStopPrice(reference, risk.getTrailingStop(), risk.getMinTrailingTicks());
    }

    /** 틱 하한 적용 전 손절(음수 비율) — PERCENT: stop-loss / ATR: -atr-multiplier × 집계봉 ATR% (계산 불가면 stop-loss). */
    private double baseStopLoss(String market) {
        TradingProperties.Risk risk = tradingProperties.getRisk();
        if (risk.getStopMode() != TradingProperties.Risk.StopMode.ATR) {
            return risk.getStopLoss();
        }
        BigDecimal atrPct = indicatorService.calculateAggregatedATRPercent(market, risk.getAtrBarMinutes(), risk.getAtrPeriod());
        if (atrPct == null) {
            log.warn("ATR stop unavailable for {} — falling back to stop-loss {}", market, risk.getStopLoss());
            return risk.getStopLoss();
        }
        return -risk.getAtrMultiplier() * atrPct.doubleValue() / 100;
    }

    /** 유효 손절(음수 비율) = min(baseStopLoss, -min-stop-ticks 틱 / 진입가). */
    private double effectiveStopLoss(String market, BigDecimal entryPrice) {
        return ExitThresholds.effectiveStopLoss(baseStopLoss(market), tradingProperties.getRisk().getMinStopTicks(), entryPrice);
    }

    /**
     * 손절가 계산 — 리스크 루프와 같은 유효 손절(틱 하한·ATR 모드)을 가격으로 (판정은 수수료차감 손익률, 이 값은 표시용).
     */
    public BigDecimal calculateStopLossPrice(BigDecimal entryPrice) {
        return ExitThresholds.stopLossPrice(entryPrice, baseStopLoss(tradingProperties.getBot().getMarket()),
                tradingProperties.getRisk().getMinStopTicks());
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
            tradingEventService.record(TradingEventLevel.CRITICAL, "EMERGENCY_CLOSE_FAILED",
                    market, "긴급 청산 실패 — 현재가 조회 불가");
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
