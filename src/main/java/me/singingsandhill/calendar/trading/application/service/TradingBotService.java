package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshot;
import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import me.singingsandhill.calendar.trading.domain.position.PositionStatus;

import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TradingBotService {

    private static final Logger log = LoggerFactory.getLogger(TradingBotService.class);

    // §8-B: 스윕 파라미터 — 전송 직후 in-flight 주문 보호(grace), 미발견 시 거래소 미도달로 간주하는 기한(expiry)
    private static final Duration SUBMITTED_SWEEP_GRACE = Duration.ofSeconds(10);
    private static final Duration SUBMITTED_EXPIRY = Duration.ofMinutes(2);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private volatile Instant lastTradeTime = null;
    private volatile Instant lastLoopAt = null;
    private volatile String lastError = null;

    private final CandleService candleService;
    private final SignalService signalService;
    private final IndicatorService indicatorService;
    private final RiskManagementService riskManagementService;
    private final RebalanceService rebalanceService;
    private final BithumbApiClient bithumbApiClient;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final TradingProperties tradingProperties;
    private final TradingEventService tradingEventService;
    private final TradingCircuitBreaker circuitBreaker;
    private final AccountSnapshotRepository accountSnapshotRepository;
    // P0-3: 영속화만 짧은 트랜잭션으로 감싼다. 주문 HTTP/sleep 은 트랜잭션 밖.
    private final TransactionTemplate txTemplate;

    public TradingBotService(CandleService candleService,
                             SignalService signalService,
                             IndicatorService indicatorService,
                             RiskManagementService riskManagementService,
                             RebalanceService rebalanceService,
                             BithumbApiClient bithumbApiClient,
                             TradeRepository tradeRepository,
                             PositionRepository positionRepository,
                             TradingProperties tradingProperties,
                             TradingEventService tradingEventService,
                             TradingCircuitBreaker circuitBreaker,
                             AccountSnapshotRepository accountSnapshotRepository,
                             PlatformTransactionManager transactionManager) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.candleService = candleService;
        this.signalService = signalService;
        this.indicatorService = indicatorService;
        this.riskManagementService = riskManagementService;
        this.rebalanceService = rebalanceService;
        this.bithumbApiClient = bithumbApiClient;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.tradingProperties = tradingProperties;
        this.tradingEventService = tradingEventService;
        this.circuitBreaker = circuitBreaker;
        this.accountSnapshotRepository = accountSnapshotRepository;
    }

    /**
     * 봇 시작
     */
    public boolean start() {
        if (running.compareAndSet(false, true)) {
            paused.set(false);
            log.info("Trading bot started");
            tradingEventService.record(TradingEventLevel.NOTICE, "BOT_STARTED",
                    tradingProperties.getBot().getMarket(), "트레이딩 봇 시작");

            // 초기 캔들 데이터 로드
            candleService.initializeCandles();

            // §8-G: 재시작 직후 미결(SUBMITTED) 주문 스윕 1회 — 재시작 공백 동안 발생한 갭 복구
            try {
                reconcileSubmittedOrders(tradingProperties.getBot().getMarket());
            } catch (Exception e) {
                log.error("Startup submitted-order sweep failed", e);
            }
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
            tradingEventService.record(TradingEventLevel.WARNING, "BOT_STOPPED",
                    tradingProperties.getBot().getMarket(), "트레이딩 봇 중지");
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
                tradingProperties.getBot().getMarket(),
                lastLoopAt,
                lastTradeTime,
                lastError
        );
    }

    /**
     * 1분 주기 메인 실행 로직
     * Issue #10: 강한 신호는 리밸런싱보다 우선
     */
    public void executeTradeLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        String market = tradingProperties.getBot().getMarket();
        log.debug("Executing trade loop for {}", market);

        try {
            // 루프 진입 시각 기록 (운영 가시성)
            this.lastLoopAt = Instant.now();

            // 0. §8-B: 미결(SUBMITTED) 주문 스윕 — 매매 판단 전에 정합화. 실패해도 리스크 체크를 막지 않는다.
            try {
                reconcileSubmittedOrders(market);
            } catch (Exception e) {
                log.error("Submitted-order sweep failed for {}", market, e);
            }

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
                this.lastTradeTime = Instant.now();  // 리밸런싱 후 쿨다운 연동
                return;
            }

            // 6. 신호에 따른 매매 실행
            executeTradeBySignal(market, signal);

            // 정상 종료 시 마지막 오류 클리어
            this.lastError = null;

        } catch (Exception e) {
            log.error("Error in trade loop", e);
            this.lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            tradingEventService.record(TradingEventLevel.WARNING, "LOOP_ERROR",
                    market, "트레이드 루프 오류: " + this.lastError);
        }
    }

    /**
     * 신호에 따른 매매 실행 - 다중 포지션 지원
     * Issue #2: 강한 SELL 신호는 수익률 무관하게 실행
     * 휩소 방지: 매매 간 쿨다운 + 최소 보유 시간 적용
     */
    public void executeTradeBySignal(String market, Signal signal) {
        // 쿨다운 체크: 마지막 거래 후 최소 간격 확인
        if (!isSignalCooldownElapsed()) {
            log.debug("Signal cooldown active, skipping trade (last trade: {})", lastTradeTime);
            return;
        }

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
                // 최소 보유 시간 체크 (휩소 방지)
                long minHoldingMinutes = tradingProperties.getBot().getMinHoldingMinutes();
                if (position.getOpenedAt() != null &&
                        ChronoUnit.MINUTES.between(position.getOpenedAt(), LocalDateTime.now()) < minHoldingMinutes) {
                    log.debug("Position {} below min holding time ({}min), skipping sell",
                            position.getId(), minHoldingMinutes);
                    continue;
                }

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
     * §8-B: cid 부착 구성이면 주문 전 Trade(SUBMITTED) 선영속화 → 응답 유실/UNKNOWN 시 틱 스윕이 수습.
     * cid 미부착 구성(v1+플래그 OFF)은 기존 Issue #1 동작(API 성공 시에만 저장) 유지.
     */
    public void executeBuy(String market, Signal signal) {
        // P0-2: 서킷브레이커 — 연속 손실/일일 손실 한도 도달 시 신규 매수 차단 (리스크 청산은 계속 허용)
        if (circuitBreaker.isEntryBlocked(dayStartEquity(market), realizedPnlToday(market))) {
            log.warn("Circuit breaker active - skipping BUY for {} (consecutive losses: {})",
                    market, circuitBreaker.getConsecutiveLosses());
            tradingEventService.record(TradingEventLevel.CRITICAL, "CIRCUIT_BREAKER", market,
                    String.format("서킷브레이커 작동 — 신규 매수 차단 (연속손실 %d회)",
                            circuitBreaker.getConsecutiveLosses()));
            return;
        }

        // §8-B: 결과 미확인(SUBMITTED) 주문이 남아 있으면 신규 매수 금지 — 스윕이 정합화하기 전 중복 진입 차단
        if (hasUnresolvedSubmitted(market)) {
            log.warn("Skipping BUY for {} - unresolved SUBMITTED order exists (awaiting sweep)", market);
            tradingEventService.record(TradingEventLevel.WARNING, "ENTRY_BLOCKED_PENDING_ORDER", market,
                    "미결 주문(SUBMITTED) 정합화 전 — 신규 매수 차단");
            return;
        }

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

        // P2-10/P2-12: 진입 가드 — 물타기 차단 + 코인 노출 상한
        Double currentPriceForGuard = bithumbApiClient.getCurrentPrice();
        if (currentPriceForGuard != null) {
            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceForGuard);
            List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);
            if (blocksAveragingDown(openPositions, currentPrice)) {
                log.info("Skipping BUY for {} - averaging-down blocked (기존 포지션 손실 중)", market);
                return;
            }
            BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
            BigDecimal coinBalance = coinAccount != null ? new BigDecimal(coinAccount.balance()) : BigDecimal.ZERO;
            BigDecimal coinValue = coinBalance.multiply(currentPrice);
            BigDecimal totalEquity = availableKrw.add(coinValue);
            if (exceedsExposureCap(coinValue, totalEquity)) {
                log.info("Skipping BUY for {} - coin exposure cap reached (코인 비중 상한)", market);
                return;
            }
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
            // §8-B: cid 부착 구성이면 주문 전송 전에 Trade(SUBMITTED, cid) 선영속화 — 응답이 유실돼도
            // 스윕이 cid 로 접수 여부를 되찾아 Position(SL/TP)까지 생성할 수 있다.
            String cid = null;
            Trade submittedTrade = null;
            if (bithumbApiClient.supportsClientOrderId()) {
                cid = bithumbApiClient.newClientOrderId();
                submittedTrade = Trade.createSubmittedBuy(cid, market, adjustedOrderAmount,
                        signal.getTotalScore(), "Auto buy signal");
                tradeRepository.save(submittedTrade);
            }

            BithumbOrderResponse response = cid != null
                    ? bithumbApiClient.placeMarketBuyOrder(adjustedOrderAmount, cid)
                    : bithumbApiClient.placeMarketBuyOrder(adjustedOrderAmount);

            if (response == null) {
                // 선영속화된 경우 SUBMITTED 로 남긴다(FAILED 아님) — 접수 여부 불명, 스윕이 판정
                log.warn("Buy order failed - null response from API{}",
                        submittedTrade != null ? " (SUBMITTED kept for sweep, cid=" + cid + ")" : "");
                return;
            }

            // §8-D: 접수 확인됐으나 체결정보 재조회 실패(v2 부분 응답). 현재가 폴백으로 DONE 처리하지
            // 않는다 — SUBMITTED 유지, 스윕이 체결정보를 확보한 뒤 Position 생성.
            if ("UNKNOWN".equals(response.state())) {
                log.warn("Buy order accepted but fill unknown (uuid={}, cid={}) - awaiting sweep",
                        response.uuid(), cid);
                tradingEventService.record(TradingEventLevel.WARNING, "ORDER_FILL_UNKNOWN", market,
                        "매수 접수됐으나 체결정보 미확보 — 스윕 대기 (cid=" + cid + ")");
                return;
            }

            log.info("Buy order placed: {}", response.uuid());

            // 수수료 추출
            BigDecimal fee = extractFee(response);

            // Issue #3: 체결가 결정 (재시도 로직 포함)
            BigDecimal entryPrice = extractExecutedPriceWithRetry(response, 3);

            // 체결가 확보 실패 시 에러 처리
            if (entryPrice == null) {
                // 선영속화된 경우 SUBMITTED 로 남음 → 스윕이 체결정보 확보 후 정합화
                log.error("Failed to determine entry price after order execution. Order uuid: {}{}",
                        response.uuid(), submittedTrade != null ? " (SUBMITTED kept for sweep)" : "");
                return;
            }

            // §8-C: 실제 체결 수량 사용. 유도값(주문금액/체결가)은 수수료/슬리피지를 무시해 장부 > 실잔고
            // 드리프트를 만들고 이후 매도 시 잔고부족을 유발한다. 실측 미확보 시에만 유도값 폴백.
            BigDecimal executedVolume = extractExecutedVolume(response);
            BigDecimal volume = executedVolume != null
                    ? executedVolume
                    : adjustedOrderAmount.divide(entryPrice, 8, RoundingMode.DOWN);
            BigDecimal stopLossPrice = riskManagementService.calculateStopLossPrice(entryPrice);
            BigDecimal takeProfitPrice = riskManagementService.calculateTakeProfitPrice(entryPrice);

            // §8-B: 선영속화된 Trade 를 DONE 으로 갱신(uuid 를 거래소 값으로 교체), 아니면 기존 생성 경로
            Trade trade;
            if (submittedTrade != null) {
                trade = submittedTrade;
                trade.assignExchangeUuid(response.uuid());
            } else {
                String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
                trade = Trade.createBuyOrder(uuid, market, entryPrice, orderAmount, "market",
                        signal.getTotalScore(), "Auto buy signal");
            }
            trade.markExecuted(entryPrice, volume, fee);

            // Position 생성 (수수료 포함)
            Position position = Position.open(
                    market, entryPrice, volume, stopLossPrice, takeProfitPrice, fee
            );

            // P0-3: 영속화만 짧은 트랜잭션 (Trade+Position 원자적 저장). 주문 HTTP/sleep 은 이미 위에서 완료.
            final Trade tradeToPersist = trade;
            txTemplate.executeWithoutResult(status -> {
                positionRepository.save(position);
                tradeToPersist.setPositionId(position.getId());
                tradeRepository.save(tradeToPersist);
            });
            log.info("Trade+Position persisted: uuid={}, entry={}, volume={}, fee={}",
                    trade.getUuid(), entryPrice, volume, fee);

            tradingEventService.record(TradingEventLevel.NOTICE, "BUY_EXECUTED", market,
                    String.format("매수 체결 — 신호 점수 %d, 가격 %s, 수량 %s",
                            signal.getTotalScore(), entryPrice.toPlainString(), volume.toPlainString()));

            // 쿨다운 갱신 (P2-11: 리밸런스 쿨다운도 갱신해 엔진 핑퐁 방지)
            this.lastTradeTime = Instant.now();
            rebalanceService.markRebalanceCooldown();

        } catch (Exception e) {
            // §8-B: 선영속화된 SUBMITTED 는 여기서도 남긴다(전송 여부 불명) — 스윕이 발견 또는 만료 처리
            log.error("Failed to execute buy order", e);
            tradingEventService.record(TradingEventLevel.WARNING, "BUY_FAILED", market,
                    "매수 실패: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    /**
     * §8-B: 시장 내 결과 미확인(SUBMITTED) 주문 존재 여부 — 존재하면 신규 매수 차단.
     */
    private boolean hasUnresolvedSubmitted(String market) {
        return tradeRepository.findByStatus(TradeStatus.SUBMITTED).stream()
                .anyMatch(t -> market.equals(t.getMarket()));
    }

    /**
     * §8-B 매도 확장: 해당 포지션에 결과 미확인(SUBMITTED) 매도가 있으면 재매도 금지 (이중 매도 방지).
     */
    private boolean hasUnresolvedSubmittedSell(Long positionId) {
        if (positionId == null) {
            return false;
        }
        return tradeRepository.findByStatus(TradeStatus.SUBMITTED).stream()
                .anyMatch(t -> t.getTradeType() == TradeType.SELL && positionId.equals(t.getPositionId()));
    }

    /**
     * §8-B: 틱 스윕 — SUBMITTED 주문을 client_order_id 재조회로 정합화한다 (executeTradeLoop 시작부).
     * 체결 확인된 매수는 Trade DONE + Position(SL/TP) 생성까지 의무 수행 — "체결됐으나 Position 없는
     * 무보호 창"을 제거한다. grace(10초) 이내는 in-flight 로 보고 건너뛴다.
     */
    public void reconcileSubmittedOrders(String market) {
        List<Trade> submitted = tradeRepository.findByStatus(TradeStatus.SUBMITTED);
        if (submitted.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Trade trade : submitted) {
            if (!market.equals(trade.getMarket()) || trade.getOrderedAt() == null) {
                continue;
            }
            Duration age = Duration.between(trade.getOrderedAt(), now);
            if (age.compareTo(SUBMITTED_SWEEP_GRACE) < 0) {
                continue;
            }
            try {
                resolveSubmittedTrade(trade, age);
            } catch (Exception e) {
                // 한 주문의 실패가 나머지 스윕을 막지 않는다
                log.error("Failed to reconcile submitted trade id={} cid={}",
                        trade.getId(), trade.getClientOrderId(), e);
            }
        }
    }

    private void resolveSubmittedTrade(Trade trade, Duration age) {
        BithumbOrderResponse order = trade.getClientOrderId() != null
                ? bithumbApiClient.getOrderByClientOrderId(trade.getClientOrderId())
                : null;

        if (order == null) {
            // 만료까지 미발견이면 거래소 미도달로 간주 (재조회는 무전송 조회라 안전)
            if (age.compareTo(SUBMITTED_EXPIRY) >= 0) {
                trade.markFailed("submitted order not found on exchange (expired)");
                tradeRepository.save(trade);
                log.warn("Submitted order expired without exchange record: cid={}", trade.getClientOrderId());
                tradingEventService.record(TradingEventLevel.WARNING, "ORDER_RECONCILE_EXPIRED",
                        trade.getMarket(), "미결 주문 만료 — 거래소 미도달로 간주 (cid=" + trade.getClientOrderId() + ")");
            }
            return; // 만료 전이면 다음 틱까지 유지
        }

        BigDecimal executedPrice = extractExecutedPrice(order);
        BigDecimal executedVolume = extractExecutedVolume(order);
        if (executedPrice != null && executedVolume != null) {
            confirmSubmittedTrade(trade, order, executedPrice, executedVolume);
            return;
        }
        if ("cancel".equalsIgnoreCase(order.state())) {
            trade.markCancelled();
            tradeRepository.save(trade);
            log.info("Submitted order reconciled as cancelled: cid={}", trade.getClientOrderId());
            tradingEventService.record(TradingEventLevel.NOTICE, "ORDER_RECONCILE_CANCELLED",
                    trade.getMarket(), "미결 주문 취소 확인 (cid=" + trade.getClientOrderId() + ")");
            return;
        }
        // wait 또는 체결정보 미확보 → 다음 틱까지 유지
        log.debug("Submitted order still pending (state={}): cid={}", order.state(), trade.getClientOrderId());
    }

    /**
     * §8-B: 스윕이 체결을 확인한 SUBMITTED 주문의 정합화 — 매수는 Trade DONE + (Position 미연결이면)
     * SL/TP 포함 Position 생성, 매도는 Trade DONE + 연결 포지션 청산. Trade+Position 은 짧은
     * 트랜잭션으로 원자 저장 (P0-3 패턴).
     */
    private void confirmSubmittedTrade(Trade trade, BithumbOrderResponse order,
                                       BigDecimal executedPrice, BigDecimal executedVolume) {
        BigDecimal fee = extractFee(order);
        trade.assignExchangeUuid(order.uuid());
        trade.markExecuted(executedPrice, executedVolume, fee);

        if (trade.getTradeType() == TradeType.SELL) {
            closeReconciledSellPosition(trade, executedPrice, executedVolume, fee);
            return;
        }

        Position position = null;
        if (trade.getPositionId() == null) {
            position = Position.open(trade.getMarket(), executedPrice, executedVolume,
                    riskManagementService.calculateStopLossPrice(executedPrice),
                    riskManagementService.calculateTakeProfitPrice(executedPrice), fee);
        }

        final Position positionToPersist = position;
        txTemplate.executeWithoutResult(status -> {
            if (positionToPersist != null) {
                positionRepository.save(positionToPersist);
                trade.setPositionId(positionToPersist.getId());
            }
            tradeRepository.save(trade);
        });
        log.info("Submitted order reconciled as filled: cid={}, uuid={}, price={}, volume={}, positionCreated={}",
                trade.getClientOrderId(), trade.getUuid(), executedPrice, executedVolume, position != null);
        tradingEventService.record(TradingEventLevel.NOTICE, "ORDER_RECONCILE_FILLED", trade.getMarket(),
                String.format("미결 주문 체결 확인 — 가격 %s, 수량 %s%s",
                        executedPrice.toPlainString(), executedVolume.toPlainString(),
                        position != null ? ", Position(SL/TP) 생성" : ""));
    }

    /**
     * §8-B 매도 확장: 스윕이 체결을 확인한 매도의 포지션 청산. 다른 경로(리스크 청산 등)가 이미 닫은
     * 포지션이면 Trade 만 정합화한다 (이중 청산 금지).
     */
    private void closeReconciledSellPosition(Trade trade, BigDecimal exitPrice,
                                             BigDecimal exitVolume, BigDecimal fee) {
        Position position = trade.getPositionId() != null
                ? positionRepository.findById(trade.getPositionId()).orElse(null)
                : null;

        if (position == null || !position.canClose()) {
            tradeRepository.save(trade);
            log.warn("Submitted sell reconciled but position {} not open — trade updated only: cid={}",
                    trade.getPositionId(), trade.getClientOrderId());
            tradingEventService.record(TradingEventLevel.NOTICE, "ORDER_RECONCILE_FILLED", trade.getMarket(),
                    "미결 매도 체결 확인 — 포지션은 이미 청산됨 (cid=" + trade.getClientOrderId() + ")");
            return;
        }

        position.close(exitPrice, exitVolume, CloseReason.SIGNAL, fee);
        final Position positionToPersist = position;
        txTemplate.executeWithoutResult(status -> {
            tradeRepository.save(trade);
            positionRepository.save(positionToPersist);
        });
        circuitBreaker.recordOutcome(position.getRealizedPnl());
        log.info("Submitted sell reconciled as filled: cid={}, uuid={}, exit={}, pnl={}%",
                trade.getClientOrderId(), trade.getUuid(), exitPrice, position.getRealizedPnlPct());
        tradingEventService.record(TradingEventLevel.NOTICE, "ORDER_RECONCILE_FILLED", trade.getMarket(),
                String.format("미결 매도 체결 확인 — 가격 %s, 포지션 %d 청산",
                        exitPrice.toPlainString(), position.getId()));
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
     * §8-C: 실제 체결 수량 추출. trades 합산(부분 체결 대응) → 응답 executed_volume 폴백 → 없으면 null.
     */
    private BigDecimal extractExecutedVolume(BithumbOrderResponse response) {
        if (response.trades() != null && !response.trades().isEmpty()) {
            BigDecimal totalVolume = BigDecimal.ZERO;
            boolean parsed = false;
            for (BithumbOrderResponse.TradeDetail trade : response.trades()) {
                if (trade.volume() != null && !trade.volume().isEmpty()) {
                    try {
                        totalVolume = totalVolume.add(new BigDecimal(trade.volume()));
                        parsed = true;
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse trade volume: {}", trade.volume());
                    }
                }
            }
            if (parsed && totalVolume.compareTo(BigDecimal.ZERO) > 0) {
                return totalVolume;
            }
        }
        if (response.executedVolume() != null && !response.executedVolume().isEmpty()) {
            try {
                BigDecimal ev = new BigDecimal(response.executedVolume());
                if (ev.compareTo(BigDecimal.ZERO) > 0) {
                    return ev;
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse executed_volume: {}", response.executedVolume());
            }
        }
        return null;
    }

    /**
     * 매도 실행
     * §8-B: cid 부착 구성이면 주문 전 Trade(SUBMITTED, positionId 연결) 선영속화 — 응답 유실 시
     * 포지션이 청산 기록 없이 이중 매도되는 갭을 스윕이 수습. 미부착 구성은 기존 Issue #1 동작 유지.
     */
    public void executeSell(String market, Signal signal, Position position) {
        log.info("Executing SELL order for position: {}", position.getId());

        // Issue #4: 포지션 상태 확인
        if (!position.canClose()) {
            log.warn("Position {} cannot be closed - status: {}", position.getId(), position.getStatus());
            return;
        }

        // §8-B: 이 포지션에 결과 미확인(SUBMITTED) 매도가 남아 있으면 재매도 금지 (이중 매도 방지)
        if (hasUnresolvedSubmittedSell(position.getId())) {
            log.warn("Skipping SELL for position {} - unresolved SUBMITTED sell exists (awaiting sweep)",
                    position.getId());
            return;
        }

        try {
            // §8-B: cid 부착 구성이면 주문 전송 전에 Trade(SUBMITTED, cid, positionId) 선영속화
            String cid = null;
            Trade submittedTrade = null;
            if (bithumbApiClient.supportsClientOrderId()) {
                cid = bithumbApiClient.newClientOrderId();
                submittedTrade = Trade.createSubmittedSell(cid, position.getId(), market,
                        position.getEntryVolume(), signal.getTotalScore(), "Auto sell signal");
                tradeRepository.save(submittedTrade);
            }

            BithumbOrderResponse response = cid != null
                    ? bithumbApiClient.placeMarketSellOrder(position.getEntryVolume(), cid)
                    : bithumbApiClient.placeMarketSellOrder(position.getEntryVolume());

            if (response == null) {
                // 선영속화된 경우 SUBMITTED 로 남긴다 — 접수 여부 불명, 스윕이 판정 (포지션은 OPEN 유지)
                log.warn("Sell order failed - null response from API{}",
                        submittedTrade != null ? " (SUBMITTED kept for sweep, cid=" + cid + ")" : "");
                return;
            }

            // §8-D: 접수 확인됐으나 체결정보 재조회 실패(v2 부분 응답) — 스윕이 체결 확인 후 포지션 청산
            if ("UNKNOWN".equals(response.state())) {
                log.warn("Sell order accepted but fill unknown (uuid={}, cid={}) - awaiting sweep",
                        response.uuid(), cid);
                tradingEventService.record(TradingEventLevel.WARNING, "ORDER_FILL_UNKNOWN", market,
                        "매도 접수됐으나 체결정보 미확보 — 스윕 대기 (cid=" + cid + ")");
                return;
            }

            log.info("Sell order placed: {}", response.uuid());

            // 수수료 추출
            BigDecimal fee = extractFee(response);

            // Issue #3: 체결가 결정 (재시도 로직 포함)
            BigDecimal exitPrice = extractExecutedPriceWithRetry(response, 3);

            // 체결가 확보 실패 시 에러 처리
            if (exitPrice == null) {
                log.error("Failed to determine exit price after order execution. Order uuid: {}{}",
                        response.uuid(), submittedTrade != null ? " (SUBMITTED kept for sweep)" : "");
                return;
            }

            // §8-B: 선영속화된 Trade 를 DONE 으로 갱신(uuid 교체), 아니면 기존 생성 경로
            Trade trade;
            if (submittedTrade != null) {
                trade = submittedTrade;
                trade.assignExchangeUuid(response.uuid());
            } else {
                String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
                trade = Trade.createSellOrder(uuid, position.getId(), market, exitPrice,
                        position.getEntryVolume(), "market", signal.getTotalScore(), "Auto sell signal");
            }
            trade.markExecuted(exitPrice, position.getEntryVolume(), fee);

            // Position 청산 (수수료 포함)
            position.close(exitPrice, position.getEntryVolume(), CloseReason.SIGNAL, fee);

            // P0-3: 영속화만 짧은 트랜잭션. 주문 HTTP/sleep 은 이미 위에서 완료.
            txTemplate.executeWithoutResult(status -> {
                tradeRepository.save(trade);
                positionRepository.save(position);
            });
            log.info("Position closed: exit={}, pnl={}%, fee={}",
                    exitPrice, position.getRealizedPnlPct(), fee);

            BigDecimal pnlPct = position.getRealizedPnlPct();
            TradingEventLevel sellLevel = pnlPct != null && pnlPct.signum() >= 0
                    ? TradingEventLevel.OK : TradingEventLevel.NOTICE;
            tradingEventService.record(sellLevel, "SELL_EXECUTED", market,
                    String.format("신호 매도 체결 — 점수 %d, 가격 %s, 손익률 %s%%",
                            signal.getTotalScore(), exitPrice.toPlainString(),
                            pnlPct != null ? pnlPct.toPlainString() : "-"));

            // P0-2: 서킷브레이커 연속 손실 스트릭 갱신
            circuitBreaker.recordOutcome(position.getRealizedPnl());

            // 쿨다운 갱신 (P2-11: 리밸런스 쿨다운도 갱신해 엔진 핑퐁 방지)
            this.lastTradeTime = Instant.now();
            rebalanceService.markRebalanceCooldown();

        } catch (IllegalStateException e) {
            // Issue #4: 이미 닫힌 포지션
            log.warn("Position {} already closed: {}", position.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to execute sell order", e);
            tradingEventService.record(TradingEventLevel.WARNING, "SELL_FAILED", market,
                    "매도 실패: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    /**
     * 수동 매수
     */
    public boolean manualBuy(BigDecimal amount) {
        String market = tradingProperties.getBot().getMarket();
        log.info("Manual buy: {} KRW", amount);

        // P0-3: 킬스위치 — bot.enabled=false 면 수동 실주문도 차단 (긴급청산 emergencyClose 는 예외).
        if (!tradingProperties.getBot().isEnabled()) {
            log.warn("Manual buy rejected - trading disabled (bot.enabled=false)");
            return false;
        }
        // P1-4: 수동 매수도 진입 리스크 가드(서킷브레이커·물타기·노출상한)를 우회하지 않는다.
        if (entryRiskGuardsBlock(market)) {
            return false;
        }

        try {
            // Issue #1: API 호출 먼저, 성공 시에만 저장
            BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(amount);
            if (response == null) {
                log.warn("Manual buy order failed - no response from API");
                return false;
            }
            BigDecimal fee = extractFee(response);
            BigDecimal entryPrice = extractExecutedPriceWithRetry(response, 3);
            if (entryPrice == null) {
                log.error("Manual buy: cannot determine entry price. uuid={}", response.uuid());
                return false;
            }
            // §8-C: 실측 체결 수량 우선(장부 드리프트 방지), 미확보 시 유도값 폴백.
            BigDecimal executedVolume = extractExecutedVolume(response);
            BigDecimal volume = executedVolume != null
                    ? executedVolume
                    : amount.divide(entryPrice, 8, RoundingMode.DOWN);
            // #3: 수동 매수도 추적 Position(SL/TP) 생성 → 리스크 루프가 보호
            BigDecimal stopLoss = riskManagementService.calculateStopLossPrice(entryPrice);
            BigDecimal takeProfit = riskManagementService.calculateTakeProfitPrice(entryPrice);

            String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
            Trade trade = Trade.createBuyOrder(uuid, market, entryPrice, amount, "market", null, "Manual buy");
            trade.markExecuted(entryPrice, volume, fee);
            Position position = Position.open(market, entryPrice, volume, stopLoss, takeProfit, fee);

            txTemplate.executeWithoutResult(status -> {
                tradeRepository.save(trade);
                positionRepository.save(position);
            });
            log.info("Manual buy: opened tracked position - entry={}, volume={}, SL={}, TP={}",
                    entryPrice, volume, stopLoss, takeProfit);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute manual buy", e);
        }
        return false;
    }

    /**
     * 수동 매도
     */
    public boolean manualSell(BigDecimal volume) {
        String market = tradingProperties.getBot().getMarket();
        log.info("Manual sell: {} coins", volume);

        // P0-3: 킬스위치 — bot.enabled=false 면 수동 실주문 차단. 포지션을 비우려면 emergencyClose 사용.
        if (!tradingProperties.getBot().isEnabled()) {
            log.warn("Manual sell rejected - trading disabled (bot.enabled=false)");
            return false;
        }

        try {
            // Issue #1: API 호출 먼저, 성공 시에만 저장
            BithumbOrderResponse response = bithumbApiClient.placeMarketSellOrder(volume);
            if (response == null) {
                log.warn("Manual sell order failed - no response from API");
                return false;
            }
            BigDecimal fee = extractFee(response);
            BigDecimal exitPrice = extractExecutedPriceWithRetry(response, 3);
            if (exitPrice == null) {
                log.error("Manual sell: cannot determine exit price. uuid={}", response.uuid());
                return false;
            }
            String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
            Trade trade = Trade.createSellOrder(uuid, null, market, exitPrice, volume, "market", null, "Manual sell");
            trade.markExecuted(exitPrice, volume, fee);
            tradeRepository.save(trade);

            // #3: 추적 OPEN 포지션을 FIFO 로 청산 기록 (추가 주문 없이 회계 정합)
            reconcilePositionsAfterManualSell(market, volume, exitPrice);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute manual sell", e);
        }
        return false;
    }

    /**
     * 긴급 청산
     */
    public void emergencyClose() {
        String market = tradingProperties.getBot().getMarket();
        log.warn("Emergency close triggered");
        tradingEventService.record(TradingEventLevel.CRITICAL, "EMERGENCY_CLOSE",
                market, "긴급 청산 요청 — 봇 중지 + 모든 포지션 시장가 청산");

        // 봇 중지
        stop();

        // 포지션 청산
        riskManagementService.emergencyClose(market);
    }

    /**
     * P0-2: 당일(KST 자정 이후) 실현손익 합계 (DB only). 손실이면 음수.
     */
    private BigDecimal realizedPnlToday(String market) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        return positionRepository.findByMarketAndStatusAndClosedAtBetween(
                        market, PositionStatus.CLOSED, startOfDay, now)
                .stream()
                .map(Position::getRealizedPnl)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * P0-2: 당일 시작 자본 (첫 계좌 스냅샷의 총자산). 스냅샷 부재 시 null → 일일 손실 가드 스킵.
     */
    private BigDecimal dayStartEquity(String market) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        return accountSnapshotRepository.findFirstByMarketAndDateRange(market, startOfDay, now)
                .map(AccountSnapshot::getTotalValueKrw)
                .orElse(null);
    }

    /**
     * P1-4: 진입(매수) 리스크 가드. 서킷브레이커·물타기 차단·코인 노출상한 중 하나라도 걸리면 true(차단).
     * 자동 매수(executeBuy)와 수동 매수(manualBuy)가 동일 가드를 통과하도록 공용화.
     * (현재가/코인잔고 조회 실패 시 물타기·노출 가드는 보수적으로 통과시키지 않고 스킵 — executeBuy 와 동일 정책.)
     */
    boolean entryRiskGuardsBlock(String market) {
        if (circuitBreaker.isEntryBlocked(dayStartEquity(market), realizedPnlToday(market))) {
            log.warn("Entry blocked - circuit breaker active ({} consecutive losses)",
                    circuitBreaker.getConsecutiveLosses());
            return true;
        }
        Double currentPriceForGuard = bithumbApiClient.getCurrentPrice();
        if (currentPriceForGuard != null) {
            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceForGuard);
            List<Position> openPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);
            if (blocksAveragingDown(openPositions, currentPrice)) {
                log.info("Entry blocked - averaging-down guard (기존 포지션 손실 중)");
                return true;
            }
            BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
            BigDecimal coinBalance = coinAccount != null ? new BigDecimal(coinAccount.balance()) : BigDecimal.ZERO;
            BigDecimal coinValue = coinBalance.multiply(currentPrice);
            BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
            BigDecimal availableKrw = krwAccount != null ? new BigDecimal(krwAccount.balance()) : BigDecimal.ZERO;
            BigDecimal totalEquity = availableKrw.add(coinValue);
            if (exceedsExposureCap(coinValue, totalEquity)) {
                log.info("Entry blocked - coin exposure cap (코인 비중 상한)");
                return true;
            }
        }
        return false;
    }

    /**
     * P2-10: 물타기 차단 — 보유 중인 OPEN 포지션 중 현재가 기준 손실인 것이 있으면 추가 매수 차단.
     */
    boolean blocksAveragingDown(List<Position> openPositions, BigDecimal currentPrice) {
        if (!tradingProperties.getBot().isBlockAveragingDown() || currentPrice == null) {
            return false;
        }
        for (Position p : openPositions) {
            if (p.getEntryPrice() != null && currentPrice.compareTo(p.getEntryPrice()) < 0) {
                return true; // 손실 중인 포지션 존재 → 물타기 차단
            }
        }
        return false;
    }

    /**
     * P2-12: 코인 노출 상한 — 코인 가치/총자본이 maxCoinExposurePct 이상이면 신규 매수 스킵.
     */
    /**
     * #3: 수동 매도 후 추적 OPEN 포지션을 FIFO 로 청산 기록 (추가 주문 없이 회계 정합 — 실제 매도는
     * 이미 manual 주문으로 체결됨). 남은 청산량이 0 이 될 때까지 오래된 포지션부터 닫는다.
     */
    void reconcilePositionsAfterManualSell(String market, BigDecimal soldVolume, BigDecimal exitPrice) {
        List<Position> open = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN).stream()
                .sorted(Comparator.comparing(Position::getOpenedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        BigDecimal remaining = soldVolume;
        for (Position p : open) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (!p.canClose()) {
                continue;
            }
            final Position pos = p;
            // 실제 매도는 manual 주문으로 이미 체결됨 → 추가 주문 없이 Position 청산 기록만 (회계 정합)
            txTemplate.executeWithoutResult(status -> {
                pos.close(exitPrice, pos.getEntryVolume(), CloseReason.MANUAL, BigDecimal.ZERO);
                positionRepository.save(pos);
            });
            circuitBreaker.recordOutcome(pos.getRealizedPnl());
            remaining = remaining.subtract(pos.getEntryVolume());
        }
    }

    boolean exceedsExposureCap(BigDecimal coinValue, BigDecimal totalEquity) {
        double cap = tradingProperties.getBot().getMaxCoinExposurePct();
        if (cap <= 0 || coinValue == null || totalEquity == null || totalEquity.signum() <= 0) {
            return false;
        }
        BigDecimal ratio = coinValue.divide(totalEquity, 6, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.valueOf(cap)) >= 0;
    }

    /**
     * 신호 쿨다운 경과 여부 확인
     */
    private boolean isSignalCooldownElapsed() {
        if (lastTradeTime == null) {
            return true;
        }
        long cooldownMinutes = tradingProperties.getBot().getSignalCooldownMinutes();
        Duration elapsed = Duration.between(lastTradeTime, Instant.now());
        return elapsed.toMinutes() >= cooldownMinutes;
    }

    /**
     * 리밸런싱 실행 후 쿨다운 갱신용 (TradingBotService.executeTradeLoop에서 호출)
     */
    public void updateLastTradeTime() {
        this.lastTradeTime = Instant.now();
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

    public record BotStatus(
            boolean running,
            boolean paused,
            String market,
            Instant lastLoopAt,
            Instant lastTradeAt,
            String lastError
    ) {
        // 기존 3파라미터 호환용
        public BotStatus(boolean running, boolean paused, String market) {
            this(running, paused, market, null, null, null);
        }
    }
}
