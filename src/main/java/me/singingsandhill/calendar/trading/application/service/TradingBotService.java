package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
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

import me.singingsandhill.calendar.trading.domain.position.PositionStatus;

import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Transactional(readOnly = true)
public class TradingBotService {

    private static final Logger log = LoggerFactory.getLogger(TradingBotService.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final CandleService candleService;
    private final SignalService signalService;
    private final IndicatorService indicatorService;
    private final RiskManagementService riskManagementService;
    private final RebalanceService rebalanceService;
    private final BithumbApiClient bithumbApiClient;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final TradingProperties tradingProperties;

    public TradingBotService(CandleService candleService,
                             SignalService signalService,
                             IndicatorService indicatorService,
                             RiskManagementService riskManagementService,
                             RebalanceService rebalanceService,
                             BithumbApiClient bithumbApiClient,
                             TradeRepository tradeRepository,
                             PositionRepository positionRepository,
                             TradingProperties tradingProperties) {
        this.candleService = candleService;
        this.signalService = signalService;
        this.indicatorService = indicatorService;
        this.riskManagementService = riskManagementService;
        this.rebalanceService = rebalanceService;
        this.bithumbApiClient = bithumbApiClient;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 봇 시작
     */
    public boolean start() {
        if (running.compareAndSet(false, true)) {
            paused.set(false);
            log.info("Trading bot started");

            // 초기 캔들 데이터 로드
            candleService.initializeCandles();
            return true;
        }
        log.warn("Trading bot is already running");
        return false;
    }

    /**
     * 봇 중지
     */
    public boolean stop() {
        if (running.compareAndSet(true, false)) {
            paused.set(false);
            log.info("Trading bot stopped");
            return true;
        }
        log.warn("Trading bot is not running");
        return false;
    }

    /**
     * 봇 일시정지
     */
    public boolean pause() {
        if (running.get() && paused.compareAndSet(false, true)) {
            log.info("Trading bot paused");
            return true;
        }
        return false;
    }

    /**
     * 봇 재개
     */
    public boolean resume() {
        if (running.get() && paused.compareAndSet(true, false)) {
            log.info("Trading bot resumed");
            return true;
        }
        return false;
    }

    /**
     * 봇 상태 조회
     */
    public BotStatus getStatus() {
        return new BotStatus(
                running.get(),
                paused.get(),
                tradingProperties.getBot().getMarket()
        );
    }

    /**
     * 1분 주기 메인 실행 로직
     * Issue #10: 강한 신호는 리밸런싱보다 우선
     */
    @Transactional
    public void executeTradeLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        String market = tradingProperties.getBot().getMarket();
        log.debug("Executing trade loop for {}", market);

        try {
            // 1. 캔들 데이터 업데이트
            candleService.fetchAndSaveCandles();

            // 2. 리스크 체크 (손절/익절) - 최우선
            CloseReason closeReason = riskManagementService.checkAndExecuteRiskRules(market);
            if (closeReason != null) {
                log.info("Position closed due to: {}", closeReason);
                return;
            }

            // 3. 신호 생성 (Issue #10: 리밸런싱 전에 신호 먼저 확인)
            Signal signal = signalService.generateSignal(market);
            if (signal == null) {
                log.warn("Failed to generate signal");
                return;
            }

            // 4. Issue #10: 강한 신호는 리밸런싱보다 우선
            int strongSignalThreshold = 60;
            boolean isStrongSignal = Math.abs(signal.getTotalScore()) >= strongSignalThreshold;

            if (isStrongSignal && signal.getSignalType() != SignalType.HOLD) {
                log.info("Strong signal detected (score: {}), prioritizing over rebalancing", signal.getTotalScore());
                executeTradeBySignal(market, signal);
                return;
            }

            // 5. 일반 신호: 리밸런싱 우선
            RebalanceService.RebalanceResult rebalanceResult = rebalanceService.checkAndExecute(market);
            if (rebalanceResult.executed()) {
                log.info("Rebalancing executed");
                return;
            }

            // 6. 신호에 따른 매매 실행
            executeTradeBySignal(market, signal);

        } catch (Exception e) {
            log.error("Error in trade loop", e);
        }
    }

    /**
     * 신호에 따른 매매 실행 - 다중 포지션 지원
     * Issue #2: 강한 SELL 신호는 수익률 무관하게 실행
     */
    @Transactional
    public void executeTradeBySignal(String market, Signal signal) {
        // 최대 포지션 수 체크
        int maxPositions = tradingProperties.getBot().getMaxPositions();
        long openPositionCount = positionRepository.countByMarketAndStatus(market, PositionStatus.OPEN);

        if (signal.getSignalType() == SignalType.BUY && openPositionCount < maxPositions) {
            executeBuy(market, signal);
        } else if (signal.getSignalType() == SignalType.SELL) {
            // 모든 열린 포지션 청산
            List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);
            Double currentPrice = bithumbApiClient.getCurrentPrice();
            double minProfitThreshold = tradingProperties.getRisk().getMinProfitThreshold();

            // Issue #2: 강한 SELL 신호 여부 확인
            boolean isStrongSellSignal = isStrongSellSignal(signal);

            for (Position position : openPositions) {
                if (currentPrice != null) {
                    // 슬리피지를 고려한 보수적 예상 매도가 계산
                    double slippageBuffer = tradingProperties.getRisk().getSlippageBuffer();
                    BigDecimal conservativeExitPrice = BigDecimal.valueOf(currentPrice)
                            .multiply(BigDecimal.valueOf(1.0 - slippageBuffer));

                    // 수수료 + 슬리피지를 포함한 실제 수익률로 판단
                    BigDecimal feeRate = BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate());
                    BigDecimal pnlPct = position.calculateUnrealizedPnlPctWithFee(conservativeExitPrice, feeRate);

                    // Issue #2: 강한 신호도 최대 손실률 제한 적용 (-2%)
                    double strongSignalMaxLoss = tradingProperties.getRisk().getStrongSignalMaxLoss();
                    BigDecimal maxLossThreshold = BigDecimal.valueOf(strongSignalMaxLoss * 100);  // -2.0%

                    if (isStrongSellSignal) {
                        if (pnlPct.compareTo(maxLossThreshold) >= 0) {
                            log.info("Strong SELL signal: pnl={}% >= {}%, executing (score: {}, divergence: {})",
                                    pnlPct, maxLossThreshold, signal.getTotalScore(), signal.hasDivergence());
                            executeSell(market, signal, position);
                        } else {
                            log.warn("Strong SELL signal skipped: pnl={}% < {}% max loss threshold (score: {}, divergence: {})",
                                    pnlPct, maxLossThreshold, signal.getTotalScore(), signal.hasDivergence());
                        }
                    } else if (pnlPct.doubleValue() >= minProfitThreshold * 100) {
                        executeSell(market, signal, position);
                    } else {
                        log.info("Skipping weak sell signal - below min profit threshold: {}% (min: {}%)",
                                pnlPct, minProfitThreshold * 100);
                    }
                } else {
                    // 가격 조회 실패 시 매도 스킵 (수익률 판단 불가)
                    log.warn("Skipping sell - cannot get current price for profit check");
                }
            }
        }
    }

    /**
     * Issue #2: 강한 SELL 신호 판별
     * - 점수 -60 이하 (강한 하락 추세)
     * - 약세 다이버전스 감지 (RSI 또는 Stochastic)
     */
    private boolean isStrongSellSignal(Signal signal) {
        // 점수 -60 이하: 강한 매도 신호
        if (signal.getTotalScore() <= -60) {
            return true;
        }
        // 약세 다이버전스 감지
        if (signal.getRsiDivergence() == DivergenceType.BEARISH
                || signal.getStochDivergence() == DivergenceType.BEARISH) {
            return true;
        }
        return false;
    }

    /**
     * 매수 실행
     * Issue #1: API 호출 먼저, 성공 시에만 Trade 저장 (고아 레코드 방지)
     */
    @Transactional
    public void executeBuy(String market, Signal signal) {
        BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
        if (krwAccount == null) {
            log.warn("Cannot get KRW balance");
            return;
        }

        BigDecimal availableKrw = new BigDecimal(krwAccount.balance());
        BigDecimal minOrderAmount = BigDecimal.valueOf(5000); // 최소 주문 금액

        if (availableKrw.compareTo(minOrderAmount) < 0) {
            log.info("Insufficient KRW balance: {}", availableKrw);
            return;
        }

        // ATR 기반 동적 비율 계산 (변동성에 따라 15~35% 조정)
        double orderRatio = calculateDynamicOrderRatio(market);
        BigDecimal orderAmount = availableKrw.multiply(BigDecimal.valueOf(orderRatio))
                .setScale(0, RoundingMode.DOWN);

        // 슬리피지 버퍼 적용 (0.5% 적게 주문하여 예상보다 적은 체결 방지)
        double slippageBuffer = tradingProperties.getRisk().getSlippageBuffer();
        BigDecimal adjustedOrderAmount = orderAmount.multiply(BigDecimal.valueOf(1.0 - slippageBuffer))
                .setScale(0, RoundingMode.DOWN);

        log.info("Executing BUY order: {} KRW (ratio: {}%, slippage buffer: {}%)",
                adjustedOrderAmount, orderRatio * 100, slippageBuffer * 100);

        try {
            // Issue #1: API 호출 먼저 실행 (슬리피지 버퍼 적용된 금액)
            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(adjustedOrderAmount);

            if (response == null) {
                log.warn("Buy order failed - null response from API");
                return;
            }

            log.info("Buy order placed: {}", response.uuid());

            // 수수료 추출
            BigDecimal fee = extractFee(response);

            // Issue #3: 체결가 결정 (재시도 로직 포함)
            BigDecimal entryPrice = extractExecutedPriceWithRetry(response, 3);

            // 체결가 확보 실패 시 에러 처리
            if (entryPrice == null) {
                log.error("Failed to determine entry price after order execution. Order uuid: {}", response.uuid());
                // API 호출은 성공했지만 체결가 확인 실패 - 로그만 남기고 종료
                // 실제로는 주문이 체결되었을 수 있으므로 별도 확인 필요
                return;
            }

            // Position 생성
            BigDecimal volume = orderAmount.divide(entryPrice, 8, RoundingMode.DOWN);
            BigDecimal stopLossPrice = riskManagementService.calculateStopLossPrice(entryPrice);
            BigDecimal takeProfitPrice = riskManagementService.calculateTakeProfitPrice(entryPrice);

            // Issue #1: API 성공 후에만 Trade 저장
            String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
            Trade trade = Trade.createBuyOrder(
                    uuid,
                    market,
                    entryPrice,
                    orderAmount,
                    "market",
                    signal.getTotalScore(),
                    "Auto buy signal"
            );
            trade.markExecuted(entryPrice, volume, fee);
            tradeRepository.save(trade);
            log.info("Trade record created: uuid={}", uuid);

            // Position 생성 (수수료 포함)
            Position position = Position.open(
                    market, entryPrice, volume, stopLossPrice, takeProfitPrice, fee
            );
            positionRepository.save(position);
            log.info("Position opened: entry={}, volume={}, fee={}", entryPrice, volume, fee);

        } catch (Exception e) {
            log.error("Failed to execute buy order", e);
        }
    }

    /**
     * Issue #3: 체결가 재시도 로직
     * 주문 응답에서 체결가를 추출하고, 실패 시 주문 상세 조회로 재시도
     */
    private BigDecimal extractExecutedPriceWithRetry(BithumbOrderResponse response, int maxRetries) {
        // 1단계: 응답에서 직접 추출
        BigDecimal price = extractExecutedPrice(response);
        if (price != null) {
            return price;
        }

        // 2단계: 주문 상세 조회로 재시도
        if (response.uuid() != null) {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(500 * (i + 1));  // 500ms, 1000ms, 1500ms 백오프
                    BithumbOrderResponse orderDetail = bithumbApiClient.getOrder(response.uuid());
                    if (orderDetail != null) {
                        price = extractExecutedPrice(orderDetail);
                        if (price != null) {
                            log.info("Retrieved execution price on retry {}: {}", i + 1, price);
                            return price;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Price extraction retry interrupted");
                    break;
                }
            }
        }

        // 3단계: 현재가로 fallback
        Double currentPrice = bithumbApiClient.getCurrentPrice();
        if (currentPrice != null) {
            log.warn("Using current price as final fallback: {}", currentPrice);
            return BigDecimal.valueOf(currentPrice);
        }

        log.error("Failed to determine execution price after {} retries", maxRetries);
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
     * BithumbOrderResponse에서 체결가 추출
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
                log.debug("Calculated weighted average price: {} (from {} trades)", avgPrice, response.trades().size());
                return avgPrice;
            }
        }
        return null;
    }

    /**
     * 매도 실행
     * Issue #1: API 호출 먼저, 성공 시에만 Trade 저장 (고아 레코드 방지)
     */
    @Transactional
    public void executeSell(String market, Signal signal, Position position) {
        log.info("Executing SELL order for position: {}", position.getId());

        // Issue #4: 포지션 상태 확인
        if (!position.canClose()) {
            log.warn("Position {} cannot be closed - status: {}", position.getId(), position.getStatus());
            return;
        }

        try {
            // Issue #1: API 호출 먼저 실행
            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (response == null) {
                log.warn("Sell order failed - null response from API");
                return;
            }

            log.info("Sell order placed: {}", response.uuid());

            // 수수료 추출
            BigDecimal fee = extractFee(response);

            // Issue #3: 체결가 결정 (재시도 로직 포함)
            BigDecimal exitPrice = extractExecutedPriceWithRetry(response, 3);

            // 체결가 확보 실패 시 에러 처리
            if (exitPrice == null) {
                log.error("Failed to determine exit price after order execution. Order uuid: {}", response.uuid());
                return;
            }

            // Issue #1: API 성공 후에만 Trade 저장
            String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
            Trade trade = Trade.createSellOrder(
                    uuid,
                    position.getId(),
                    market,
                    exitPrice,
                    position.getEntryVolume(),
                    "market",
                    signal.getTotalScore(),
                    "Auto sell signal"
            );
            trade.markExecuted(exitPrice, position.getEntryVolume(), fee);
            tradeRepository.save(trade);
            log.info("Trade record created: uuid={}", uuid);

            // Position 청산 (수수료 포함)
            position.close(exitPrice, position.getEntryVolume(), CloseReason.SIGNAL, fee);
            positionRepository.save(position);
            log.info("Position closed: exit={}, pnl={}%, fee={}",
                    exitPrice, position.getRealizedPnlPct(), fee);

        } catch (IllegalStateException e) {
            // Issue #4: 이미 닫힌 포지션
            log.warn("Position {} already closed: {}", position.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to execute sell order", e);
        }
    }

    /**
     * 수동 매수
     */
    @Transactional
    public boolean manualBuy(BigDecimal amount) {
        String market = tradingProperties.getBot().getMarket();
        log.info("Manual buy: {} KRW", amount);

        // 현재가 조회 (Trade 생성용)
        Double orderPriceDouble = bithumbApiClient.getCurrentPrice();
        if (orderPriceDouble == null) {
            log.warn("Cannot get current price for manual buy");
            return false;
        }
        BigDecimal orderPrice = BigDecimal.valueOf(orderPriceDouble);

        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createBuyOrder(
                uuid,
                market,
                orderPrice,
                amount,
                "market",
                null,  // 수동 매수는 신호 점수 없음
                "Manual buy"
        );

        try {
            Trade savedTrade = tradeRepository.save(trade);
            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(amount);
            if (response != null) {
                log.info("Manual buy order placed: {}", response.uuid());
                BigDecimal fee = extractFee(response);
                Double currentPrice = bithumbApiClient.getCurrentPrice();
                if (currentPrice != null) {
                    BigDecimal price = BigDecimal.valueOf(currentPrice);
                    BigDecimal volume = amount.divide(price, 8, RoundingMode.DOWN);
                    savedTrade.markExecuted(price, volume, fee);
                    tradeRepository.save(savedTrade);
                }
                return true;
            } else {
                savedTrade.markCancelled();
                tradeRepository.save(savedTrade);
            }
        } catch (Exception e) {
            log.error("Failed to execute manual buy", e);
            trade.markFailed(e.getMessage());
            try { tradeRepository.save(trade); } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * 수동 매도
     */
    @Transactional
    public boolean manualSell(BigDecimal volume) {
        String market = tradingProperties.getBot().getMarket();
        log.info("Manual sell: {} coins", volume);

        // 현재가 조회 (Trade 생성용)
        Double orderPriceDouble = bithumbApiClient.getCurrentPrice();
        if (orderPriceDouble == null) {
            log.warn("Cannot get current price for manual sell");
            return false;
        }
        BigDecimal orderPrice = BigDecimal.valueOf(orderPriceDouble);

        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createSellOrder(
                uuid,
                null,  // 수동 매도는 포지션 ID 없음
                market,
                orderPrice,
                volume,
                "market",
                null,  // 수동 매도는 신호 점수 없음
                "Manual sell"
        );

        try {
            Trade savedTrade = tradeRepository.save(trade);
            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(volume);
            if (response != null) {
                log.info("Manual sell order placed: {}", response.uuid());
                BigDecimal fee = extractFee(response);
                Double currentPrice = bithumbApiClient.getCurrentPrice();
                if (currentPrice != null) {
                    BigDecimal price = BigDecimal.valueOf(currentPrice);
                    savedTrade.markExecuted(price, volume, fee);
                    tradeRepository.save(savedTrade);
                }
                return true;
            } else {
                savedTrade.markCancelled();
                tradeRepository.save(savedTrade);
            }
        } catch (Exception e) {
            log.error("Failed to execute manual sell", e);
            trade.markFailed(e.getMessage());
            try { tradeRepository.save(trade); } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * 긴급 청산
     */
    @Transactional
    public void emergencyClose() {
        String market = tradingProperties.getBot().getMarket();
        log.warn("Emergency close triggered");

        // 봇 중지
        stop();

        // 포지션 청산
        riskManagementService.emergencyClose(market);
    }

    /**
     * ATR 기반 동적 주문 비율 계산
     * 변동성 높음(ATR% > 3%): 15%, 보통(1-3%): 선형 보간, 낮음(< 1%): 35%
     *
     * @param market 마켓
     * @return 동적 주문 비율 (0.15 ~ 0.35)
     */
    private double calculateDynamicOrderRatio(String market) {
        BigDecimal atrPercent = indicatorService.calculateATRPercent(market);

        if (atrPercent == null) {
            log.debug("ATR calculation failed, using default ratio");
            return tradingProperties.getBot().getOrderRatio();  // 기본값 25%
        }

        double atrPct = atrPercent.doubleValue();
        double ratioMin = tradingProperties.getBot().getOrderRatioMin();  // 0.15 (변동성 높을 때)
        double ratioMax = tradingProperties.getBot().getOrderRatioMax();  // 0.35 (변동성 낮을 때)
        double baseRatio = tradingProperties.getBot().getOrderRatio();    // 0.25 (기본값)

        double result;
        if (atrPct >= 3.0) {
            // 변동성 높음 → 보수적 매수 (15%)
            result = ratioMin;
            log.debug("High volatility (ATR {}%), using min ratio {}%", atrPct, ratioMin * 100);
        } else if (atrPct <= 1.0) {
            // 변동성 낮음 → 적극적 매수 (35%)
            result = ratioMax;
            log.debug("Low volatility (ATR {}%), using max ratio {}%", atrPct, ratioMax * 100);
        } else {
            // 중간 영역 → 선형 보간
            // ATR 3% → 15%, ATR 1% → 35%
            result = ratioMax - ((atrPct - 1.0) / 2.0) * (ratioMax - ratioMin);
            log.debug("Medium volatility (ATR {}%), using interpolated ratio {}%", atrPct, result * 100);
        }

        return result;
    }

    public record BotStatus(boolean running, boolean paused, String market) {}
}
