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
    private final RiskManagementService riskManagementService;
    private final RebalanceService rebalanceService;
    private final BithumbApiClient bithumbApiClient;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final TradingProperties tradingProperties;

    public TradingBotService(CandleService candleService,
                             SignalService signalService,
                             RiskManagementService riskManagementService,
                             RebalanceService rebalanceService,
                             BithumbApiClient bithumbApiClient,
                             TradeRepository tradeRepository,
                             PositionRepository positionRepository,
                             TradingProperties tradingProperties) {
        this.candleService = candleService;
        this.signalService = signalService;
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

            // 2. 리스크 체크 (손절/익절)
            CloseReason closeReason = riskManagementService.checkAndExecuteRiskRules(market);
            if (closeReason != null) {
                log.info("Position closed due to: {}", closeReason);
                return;
            }

            // 3. 리밸런싱 체크
            RebalanceService.RebalanceResult rebalanceResult = rebalanceService.checkAndExecute(market);
            if (rebalanceResult.executed()) {
                log.info("Rebalancing executed");
                return;
            }

            // 4. 신호 생성 및 분석
            Signal signal = signalService.generateSignal(market);
            if (signal == null) {
                log.warn("Failed to generate signal");
                return;
            }

            // 5. 신호에 따른 매매 실행
            executeTradeBySignal(market, signal);

        } catch (Exception e) {
            log.error("Error in trade loop", e);
        }
    }

    /**
     * 신호에 따른 매매 실행 - 다중 포지션 지원
     */
    @Transactional
    public void executeTradeBySignal(String market, Signal signal) {
        // 최대 포지션 수 체크
        int maxPositions = tradingProperties.getBot().getMaxPositions();
        long openPositionCount = positionRepository.countByMarketAndStatus(market, PositionStatus.OPEN);

        if (signal.getSignalType() == SignalType.BUY && openPositionCount < maxPositions) {
            executeBuy(market, signal);
        } else if (signal.getSignalType() == SignalType.SELL) {
            // 모든 열린 포지션 청산 (수익성 체크 포함)
            List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);
            Double currentPrice = bithumbApiClient.getCurrentPrice();
            double minProfitThreshold = tradingProperties.getRisk().getMinProfitThreshold();

            for (Position position : openPositions) {
                if (currentPrice != null) {
                    BigDecimal pnlPct = position.calculateUnrealizedPnlPct(BigDecimal.valueOf(currentPrice));
                    // 최소 수익률(0.6%) 이상일 때만 매도 실행
                    if (pnlPct.doubleValue() >= minProfitThreshold * 100) {
                        executeSell(market, signal, position);
                    } else {
                        log.info("Skipping sell - below min profit threshold: {}% (min: {}%)",
                                pnlPct, minProfitThreshold * 100);
                    }
                } else {
                    // 가격 조회 실패 시에도 신호가 강하면 매도
                    executeSell(market, signal, position);
                }
            }
        }
    }

    /**
     * 매수 실행
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

        // 가용 자금의 25% 사용 (분할 매수)
        double orderRatio = tradingProperties.getBot().getOrderRatio();
        BigDecimal orderAmount = availableKrw.multiply(BigDecimal.valueOf(orderRatio))
                .setScale(0, RoundingMode.DOWN);

        log.info("Executing BUY order: {} KRW", orderAmount);

        // Trade 먼저 생성 (WAIT 상태)
        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createBuyOrder(
                uuid,
                market,
                null, // 시장가 주문
                orderAmount,
                "market",
                signal.getTotalScore(),
                "Auto buy signal"
        );

        try {
            Trade savedTrade = tradeRepository.save(trade);
            log.info("Trade record created: uuid={}", uuid);

            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(orderAmount);

            if (response != null) {
                log.info("Buy order placed: {}", response.uuid());

                // 수수료 추출
                BigDecimal fee = extractFee(response);

                // 체결가 결정: trades 리스트에서 가져오거나, 현재가로 fallback
                BigDecimal entryPrice = extractExecutedPrice(response);

                // 체결가 확보 실패 시 현재가 조회
                if (entryPrice == null) {
                    Double currentPrice = bithumbApiClient.getCurrentPrice();
                    if (currentPrice != null) {
                        entryPrice = BigDecimal.valueOf(currentPrice);
                        log.debug("Using current price as fallback: {}", entryPrice);
                    }
                }

                // 체결가 확보 실패 시 에러 처리
                if (entryPrice == null) {
                    log.error("Failed to determine entry price after order execution. Order uuid: {}", response.uuid());
                    savedTrade.markFailed("Price fetch failed after order execution");
                    tradeRepository.save(savedTrade);
                    return;
                }

                // Position 생성
                BigDecimal volume = orderAmount.divide(entryPrice, 8, RoundingMode.DOWN);
                BigDecimal stopLossPrice = riskManagementService.calculateStopLossPrice(entryPrice);
                BigDecimal takeProfitPrice = riskManagementService.calculateTakeProfitPrice(entryPrice);

                // Trade 실행 정보 업데이트
                savedTrade.markExecuted(entryPrice, volume, fee);
                tradeRepository.save(savedTrade);

                // Position 생성 (수수료 포함)
                Position position = Position.open(
                        market, entryPrice, volume, stopLossPrice, takeProfitPrice, fee
                );
                positionRepository.save(position);
                log.info("Position opened: entry={}, volume={}, fee={}", entryPrice, volume, fee);
            } else {
                // 주문 실패: CANCEL 상태로 변경
                savedTrade.markCancelled();
                tradeRepository.save(savedTrade);
                log.warn("Buy order cancelled - null response");
            }
        } catch (Exception e) {
            log.error("Failed to execute buy order", e);
            // 예외 발생: FAILED 상태로 변경
            trade.markFailed(e.getMessage());
            try {
                tradeRepository.save(trade);
            } catch (Exception saveEx) {
                log.error("Failed to save trade failure record", saveEx);
            }
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
     * BithumbOrderResponse에서 체결가 추출
     * trades 리스트의 첫 번째 체결 정보에서 price 가져옴
     */
    private BigDecimal extractExecutedPrice(BithumbOrderResponse response) {
        if (response.trades() != null && !response.trades().isEmpty()) {
            // 첫 번째 체결 정보에서 가격 추출
            BithumbOrderResponse.TradeDetail firstTrade = response.trades().get(0);
            if (firstTrade.price() != null && !firstTrade.price().isEmpty()) {
                try {
                    BigDecimal price = new BigDecimal(firstTrade.price());
                    log.debug("Extracted executed price from trades: {}", price);
                    return price;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse trade price: {}", firstTrade.price());
                }
            }
        }
        return null;
    }

    /**
     * 매도 실행
     */
    @Transactional
    public void executeSell(String market, Signal signal, Position position) {
        log.info("Executing SELL order for position: {}", position.getId());

        // Trade 먼저 생성 (WAIT 상태)
        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createSellOrder(
                uuid,
                position.getId(),
                market,
                BigDecimal.valueOf(signal.getCurrentPrice().doubleValue()),
                position.getEntryVolume(),
                "market",
                signal.getTotalScore(),
                "Auto sell signal"
        );

        try {
            Trade savedTrade = tradeRepository.save(trade);
            log.info("Trade record created: uuid={}", uuid);

            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (response != null) {
                log.info("Sell order placed: {}", response.uuid());

                // 수수료 추출
                BigDecimal fee = extractFee(response);

                // 체결가 결정: trades 리스트에서 가져오거나, 현재가로 fallback
                BigDecimal exitPrice = extractExecutedPrice(response);

                // 체결가 확보 실패 시 현재가 조회
                if (exitPrice == null) {
                    Double currentPrice = bithumbApiClient.getCurrentPrice();
                    if (currentPrice != null) {
                        exitPrice = BigDecimal.valueOf(currentPrice);
                        log.debug("Using current price as fallback: {}", exitPrice);
                    }
                }

                // 체결가 확보 실패 시 에러 처리
                if (exitPrice == null) {
                    log.error("Failed to determine exit price after order execution. Order uuid: {}", response.uuid());
                    savedTrade.markFailed("Price fetch failed after order execution");
                    tradeRepository.save(savedTrade);
                    return;
                }

                // Trade 실행 정보 업데이트
                savedTrade.markExecuted(exitPrice, position.getEntryVolume(), fee);
                tradeRepository.save(savedTrade);

                // Position 청산 (수수료 포함)
                position.close(exitPrice, position.getEntryVolume(), CloseReason.SIGNAL, fee);
                positionRepository.save(position);
                log.info("Position closed: exit={}, pnl={}%, fee={}",
                        exitPrice, position.getRealizedPnlPct(), fee);
            } else {
                // 주문 실패: CANCEL 상태로 변경
                savedTrade.markCancelled();
                tradeRepository.save(savedTrade);
                log.warn("Sell order cancelled - null response");
            }
        } catch (Exception e) {
            log.error("Failed to execute sell order", e);
            // 예외 발생: FAILED 상태로 변경
            trade.markFailed(e.getMessage());
            try {
                tradeRepository.save(trade);
            } catch (Exception saveEx) {
                log.error("Failed to save trade failure record", saveEx);
            }
        }
    }

    /**
     * 수동 매수
     */
    @Transactional
    public boolean manualBuy(BigDecimal amount) {
        String market = tradingProperties.getBot().getMarket();
        log.info("Manual buy: {} KRW", amount);

        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createBuyOrder(
                uuid,
                market,
                null,  // 시장가 주문
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

        String uuid = UUID.randomUUID().toString();
        Trade trade = Trade.createSellOrder(
                uuid,
                null,  // 수동 매도는 포지션 ID 없음
                market,
                null,  // 시장가 주문
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

    public record BotStatus(boolean running, boolean paused, String market) {}
}
