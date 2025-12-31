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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
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
     * 신호에 따른 매매 실행
     */
    @Transactional
    public void executeTradeBySignal(String market, Signal signal) {
        Optional<Position> openPosition = positionRepository.findOpenPositionByMarket(market);

        if (signal.getSignalType() == SignalType.BUY && openPosition.isEmpty()) {
            executeBuy(market, signal);
        } else if (signal.getSignalType() == SignalType.SELL && openPosition.isPresent()) {
            executeSell(market, signal, openPosition.get());
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

        // 가용 자금의 20% 사용 (분할 매수)
        BigDecimal orderAmount = availableKrw.multiply(BigDecimal.valueOf(0.2))
                .setScale(0, RoundingMode.DOWN);

        log.info("Executing BUY order: {} KRW", orderAmount);

        try {
            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(orderAmount);

            if (response != null) {
                log.info("Buy order placed: {}", response.uuid());

                // Trade 기록
                Trade trade = Trade.createBuyOrder(
                        response.uuid(),
                        market,
                        null, // 시장가 주문
                        orderAmount,
                        "market",
                        signal.getTotalScore(),
                        "Auto buy signal"
                );
                tradeRepository.save(trade);

                // Position 생성
                Double currentPrice = bithumbApiClient.getCurrentPrice();
                if (currentPrice != null) {
                    BigDecimal entryPrice = BigDecimal.valueOf(currentPrice);
                    BigDecimal volume = orderAmount.divide(entryPrice, 8, RoundingMode.DOWN);
                    BigDecimal stopLossPrice = riskManagementService.calculateStopLossPrice(entryPrice);
                    BigDecimal takeProfitPrice = riskManagementService.calculateTakeProfitPrice(entryPrice);

                    Position position = Position.open(
                            market, entryPrice, volume, stopLossPrice, takeProfitPrice
                    );
                    positionRepository.save(position);
                    log.info("Position opened: entry={}, volume={}", currentPrice, volume);
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute buy order", e);
        }
    }

    /**
     * 매도 실행
     */
    @Transactional
    public void executeSell(String market, Signal signal, Position position) {
        log.info("Executing SELL order for position: {}", position.getId());

        try {
            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (response != null) {
                log.info("Sell order placed: {}", response.uuid());

                // Trade 기록
                Trade trade = Trade.createSellOrder(
                        response.uuid(),
                        position.getId(),
                        market,
                        BigDecimal.valueOf(signal.getCurrentPrice().doubleValue()),
                        position.getEntryVolume(),
                        "market",
                        signal.getTotalScore(),
                        "Auto sell signal"
                );
                tradeRepository.save(trade);

                // Position 청산
                Double currentPrice = bithumbApiClient.getCurrentPrice();
                if (currentPrice != null) {
                    position.close(BigDecimal.valueOf(currentPrice), position.getEntryVolume(), CloseReason.SIGNAL);
                    positionRepository.save(position);
                    log.info("Position closed: exit={}, pnl={}%",
                            currentPrice, position.getRealizedPnlPct());
                }
            }
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

        try {
            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(amount);
            if (response != null) {
                log.info("Manual buy order placed: {}", response.uuid());

                Trade trade = Trade.createBuyOrder(
                        response.uuid(),
                        market,
                        null,  // 시장가 주문
                        amount,
                        "market",
                        null,  // 수동 매수는 신호 점수 없음
                        "Manual buy"
                );
                tradeRepository.save(trade);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to execute manual buy", e);
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

        try {
            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(volume);
            if (response != null) {
                log.info("Manual sell order placed: {}", response.uuid());

                Trade trade = Trade.createSellOrder(
                        response.uuid(),
                        null,  // 수동 매도는 포지션 ID 없음
                        market,
                        null,  // 시장가 주문
                        volume,
                        "market",
                        null,  // 수동 매도는 신호 점수 없음
                        "Manual sell"
                );
                tradeRepository.save(trade);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to execute manual sell", e);
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
