package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RebalanceService {

    private static final Logger log = LoggerFactory.getLogger(RebalanceService.class);

    private volatile Instant lastRebalanceTime = null;

    private final BithumbApiClient bithumbApiClient;
    private final IndicatorService indicatorService;
    private final TradingProperties tradingProperties;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final TradingEventService tradingEventService;
    private final RiskManagementService riskManagementService;
    // P0-3b: 영속화만 짧은 트랜잭션. 주문 HTTP 는 트랜잭션 밖.
    private final TransactionTemplate txTemplate;
    private final DailyRegimeService dailyRegimeService;
    /** 같은 사유의 연속 스킵은 60분에 1회만 기록 (10/11 밤 REBALANCE_SKIPPED 162회, ADR trading/risk/0007). */
    private final EventThrottle skipThrottle = new EventThrottle(Duration.ofMinutes(60));

    public RebalanceService(BithumbApiClient bithumbApiClient,
                            IndicatorService indicatorService,
                            TradingProperties tradingProperties,
                            TradeRepository tradeRepository,
                            PositionRepository positionRepository,
                            TradingEventService tradingEventService,
                            RiskManagementService riskManagementService,
                            PlatformTransactionManager transactionManager,
                            DailyRegimeService dailyRegimeService) {
        this.bithumbApiClient = bithumbApiClient;
        this.indicatorService = indicatorService;
        this.tradingProperties = tradingProperties;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.tradingEventService = tradingEventService;
        this.riskManagementService = riskManagementService;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.dailyRegimeService = dailyRegimeService;
    }

    /**
     * P1-3: 리밸런싱 매도 — OPEN 포지션을 FIFO(오래된 것부터) 로 청산.
     * 각 포지션 수수료차감 PnL ≥ min-sell-pnl 일 때만 청산(적자 청산 방지),
     * 목표 매도량(targetVolume) 도달 시 중단. 청산은 RiskManagementService 재사용.
     *
     * 레짐 출처가 DAILY 면 (ADR trading/risk/0007, 구멍 1) REBALANCE 포지션을 먼저 청산하고, 다른 레짐에서 산 REBALANCE
     * 포지션(= 레짐 전환으로 목표 비중이 바뀐 매도)은 '수익일 때만' 가드를 무시한다 — 가드는 같은 레짐 안의 편차 매도에만
     * 적용한다. 그러지 않으면 약세 전환 뒤에도 손실 중인 70% 를 비상 손절까지 들고 간다. 신호 포지션은 항상 가드 유지.
     * @return 실제 청산된 코인 수량 합계
     */
    BigDecimal sellByClosingProfitablePositions(String market, BigDecimal targetVolume, BigDecimal currentPrice) {
        List<Position> open = positionRepository.findByMarketAndStatus(market, PositionStatus.OPEN);
        if (open.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal feeRate = BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate());
        BigDecimal minSellPnl = BigDecimal.valueOf(tradingProperties.getRebalancing().getMinSellPnlPct() * 100);
        boolean daily = isDailyRegimeSource();
        MarketRegime current = daily ? dailyRegimeService.currentRegime() : null;

        // FIFO: 오래된 포지션부터 (DAILY 면 REBALANCE 포지션이 먼저)
        Comparator<Position> fifo = Comparator.comparing(Position::getOpenedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));
        List<Position> ordered = open.stream()
                .sorted(daily
                        ? Comparator.comparing((Position p) -> p.getEntrySource() != EntrySource.REBALANCE)
                                .thenComparing(fifo)
                        : fifo)
                .toList();

        BigDecimal sold = BigDecimal.ZERO;
        for (Position p : ordered) {
            if (sold.compareTo(targetVolume) >= 0) {
                break;
            }
            boolean regimeSwitchSell = daily && boughtUnderOtherRegime(p, current);
            if (!regimeSwitchSell) {
                BigDecimal pnlPct = p.calculateUnrealizedPnlPctWithFee(currentPrice, feeRate);
                if (pnlPct.compareTo(minSellPnl) < 0) {
                    log.debug("Rebalance sell: skipping position {} - pnl {}% < {}% (적자 청산 방지)",
                            p.getId(), pnlPct, minSellPnl);
                    continue;
                }
            }
            riskManagementService.closePosition(p, currentPrice, CloseReason.REBALANCE);
            sold = sold.add(p.getEntryVolume());
        }
        return sold;
    }

    /** 다른 레짐에서 산 REBALANCE 포지션인지 — 레짐을 모르면(UNKNOWN) 아니라고 본다(가드 유지). */
    private boolean boughtUnderOtherRegime(Position p, MarketRegime current) {
        if (p.getEntrySource() != EntrySource.REBALANCE || p.getOpenedAt() == null
                || current == null || current == MarketRegime.UNKNOWN) {
            return false;
        }
        MarketRegime atOpen = dailyRegimeService.regimeInEffectAt(p.getOpenedAt());
        return atOpen != null && atOpen != MarketRegime.UNKNOWN && atOpen != current;
    }

    /**
     * 구멍 2 (ADR trading/risk/0007): 비상 손절이 나간 뒤에는 다음 레짐 전환 전까지 리밸런싱 매수를 막는다 — 막지 않으면
     * 비중 0 → 편차 초과 → 쿨다운 뒤 재매수로 -15% 단위 루프가 되살아난다. 마지막 비상 손절 포지션(DB)과 일봉 레짐 이력으로
     * 판정하므로 재시작해도 그대로 복원된다.
     */
    private boolean buyBlockedAfterCatastrophicStop(String market) {
        Optional<Position> last = positionRepository.findLatestClosedByMarketAndCloseReason(
                market, CloseReason.CATASTROPHIC_STOP);
        if (last.isEmpty() || last.get().getClosedAt() == null) {
            return false;
        }
        return !dailyRegimeService.hasRegimeChangedAfter(last.get().getClosedAt());
    }

    private boolean isDailyRegimeSource() {
        return tradingProperties.getRebalancing().getRegimeSource() == TradingProperties.Rebalancing.RegimeSource.DAILY;
    }

    /** 같은 사유의 스킵은 60분에 1회만 기록한다. */
    private void recordSkip(String market, String message) {
        int skipped = skipThrottle.tryRecord(message, Instant.now());
        if (skipped < 0) {
            return;
        }
        tradingEventService.record(TradingEventLevel.NOTICE, "REBALANCE_SKIPPED", market,
                skipped > 0 ? message + " (직전 기록 이후 " + skipped + "회 같은 사유 생략)" : message);
    }

    /**
     * P1-3: 리밸런싱 매수 — 실주문 후 추적 Position 을 생성(SL/TP 포함)하여 리스크 루프가 보호하게 한다.
     * @return 성공 여부
     */
    boolean buyAndOpenPosition(String market, BigDecimal krwAmount, BigDecimal currentPrice) {
        BithumbOrderResponse response = bithumbApiClient.placeMarketBuyOrder(krwAmount);
        if (response == null) {
            log.warn("Rebalance buy failed - no response from API");
            return false;
        }
        BigDecimal fee = extractFee(response);
        // #4 fix: 실체결가 사용 (파라미터 currentPrice 가 아닌 — 슬리피지/시뮬레이션 체결가 반영)
        BigDecimal entryPrice = extractExecutedPrice(response);
        if (entryPrice == null) {
            entryPrice = currentPrice; // 체결가 미확보 시 파라미터 폴백
        }
        BigDecimal volume = krwAmount.divide(entryPrice, 8, RoundingMode.DOWN);
        BigDecimal stopLoss = riskManagementService.calculateStopLossPrice(entryPrice);
        BigDecimal takeProfit = riskManagementService.calculateTakeProfitPrice(entryPrice);

        String uuid = response.uuid() != null ? response.uuid() : UUID.randomUUID().toString();
        Trade trade = Trade.createBuyOrder(uuid, market, entryPrice, krwAmount, "market", null, "Rebalancing buy");
        trade.markExecuted(entryPrice, volume, fee);

        // P1-3: 리밸런스 매수도 추적 Position 생성 → 리스크 루프(SL/TP/트레일링)가 보호
        Position position = Position.open(market, entryPrice, volume, stopLoss, takeProfit, fee);
        position.setEntrySource(EntrySource.REBALANCE);   // ADR trading/observability/0004

        // P0-3b: 영속화만 짧은 트랜잭션 (주문 HTTP 는 위에서 완료).
        // Position 을 먼저 저장해 id 를 받고 Trade 에 연결한다 — 미연결이면 분석이 신호 진입으로 오분류한다.
        txTemplate.executeWithoutResult(status -> {
            positionRepository.save(position);
            trade.setPositionId(position.getId());
            tradeRepository.save(trade);
        });
        log.info("Rebalance buy: opened tracked position - volume {}, SL {}, TP {}, fee {}",
                volume, stopLoss, takeProfit, fee);
        return true;
    }

    /**
     * 리밸런싱 필요 여부 확인 및 실행
     */
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
     * P2-11: 외부(신호) 매매 실행 시 리밸런스 쿨다운도 갱신 — 엔진 핑퐁 방지.
     */
    public void markRebalanceCooldown() {
        this.lastRebalanceTime = Instant.now();
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
     * DAILY(기본, ADR trading/risk/0007): 일봉 레짐 BULL → bull-ratio, BEAR → bear-ratio, UNKNOWN → 데이터 부족 처리.
     * MINUTE_MA60(레거시): 현재가 vs 1분봉 MA60(=60분 평균) — 10/11~10/20 목표를 70%↔30% 로 매번 뒤집었다.
     * @return 목표 비중. 데이터 부족 시 skipWhenDataInsufficient=true이면 null 반환
     */
    private BigDecimal determineTargetRatio(String market) {
        if (isDailyRegimeSource()) {
            MarketRegime regime = dailyRegimeService.currentRegime();
            if (regime == MarketRegime.BULL) {
                return BigDecimal.valueOf(tradingProperties.getRebalancing().getBullRatio());
            }
            if (regime == MarketRegime.BEAR) {
                return BigDecimal.valueOf(tradingProperties.getRebalancing().getBearRatio());
            }
            if (tradingProperties.getRebalancing().isSkipWhenDataInsufficient()) {
                log.warn("Daily regime unknown for rebalancing - skipping");
                return null;
            }
            return BigDecimal.valueOf(tradingProperties.getRebalancing().getDefaultRatio());
        }

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
                if (buyBlockedAfterCatastrophicStop(market)) {
                    log.warn("Rebalance buy blocked: catastrophic stop occurred and regime has not changed since");
                    recordSkip(market, "리밸런싱 매수 차단 — 비상 손절 이후 일봉 레짐 전환 전 (재진입 루프 방지)");
                    return new RebalanceResult(false, currentRatio, targetRatio,
                            currentRatio.subtract(targetRatio).abs(),
                            "Skipped: blocked after catastrophic stop until regime change");
                }
                // 코인 비중 부족 → 매수 (P1-3: 추적 Position 생성, SL/TP 포함)
                boolean ok = buyAndOpenPosition(market, adjustedDifference.abs(), currentPrice);
                if (!ok) {
                    return new RebalanceResult(false, currentRatio, targetRatio,
                            currentRatio.subtract(targetRatio).abs());
                }
            } else {
                // 코인 비중 과다 → 매도 (P1-3: OPEN 포지션 FIFO 청산, 수익 포지션만 — 적자 청산 방지)
                BigDecimal targetSellVolume = adjustedDifference.abs().divide(currentPrice, 8, RoundingMode.DOWN);
                BigDecimal sold = sellByClosingProfitablePositions(market, targetSellVolume, currentPrice);
                if (sold.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Rebalance sell skipped: no eligible (profitable) positions to close");
                    recordSkip(market, "리밸런싱 매도 스킵 — 청산 가능한(수익) 포지션 없음 (적자 청산 방지)");
                    return new RebalanceResult(false, currentRatio, targetRatio,
                            currentRatio.subtract(targetRatio).abs(),
                            "Skipped: no profitable positions to close");
                }
            }

            // 쿨다운 시간 갱신
            lastRebalanceTime = Instant.now();
            log.info("Rebalance executed successfully. Next rebalance available after {} minutes",
                    tradingProperties.getRebalancing().getCooldownMinutes());

            String direction = difference.compareTo(BigDecimal.ZERO) > 0 ? "매수" : "매도";
            tradingEventService.record(TradingEventLevel.NOTICE, "REBALANCE_EXECUTED", market,
                    String.format("리밸런싱 %s 체결 — 현재 %s%% → 목표 %s%% (편차 %s%%)",
                            direction,
                            currentRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                            targetRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                            currentRatio.subtract(targetRatio).multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP)));

            return new RebalanceResult(true, currentRatio, targetRatio,
                    currentRatio.subtract(targetRatio).abs());
        } catch (Exception e) {
            // Issue #1: Trade는 API 성공 후에만 저장하므로 별도의 상태 업데이트 불필요
            log.error("Failed to execute rebalance: {}", e.getMessage(), e);
            tradingEventService.record(TradingEventLevel.WARNING, "REBALANCE_FAILED", market,
                    "리밸런싱 실패: " + e.getClass().getSimpleName() + " " + e.getMessage());
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

    /**
     * #4: BithumbOrderResponse 의 trades 가중평균 체결가 추출 (부분체결 대응). 없으면 null.
     */
    private BigDecimal extractExecutedPrice(BithumbOrderResponse response) {
        if (response.trades() == null || response.trades().isEmpty()) {
            return null;
        }
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
                    log.warn("Failed to parse rebalance trade: price={}, volume={}", trade.price(), trade.volume());
                }
            }
        }
        return totalVolume.compareTo(BigDecimal.ZERO) > 0
                ? totalValue.divide(totalVolume, 8, RoundingMode.HALF_UP)
                : null;
    }

    public record RebalanceResult(
        boolean executed,
        BigDecimal currentRatio,
        BigDecimal targetRatio,
        BigDecimal deviation,
        String reason
    ) {
        // 기존 4파라미터 생성자 호환용
        public RebalanceResult(boolean executed, BigDecimal currentRatio, BigDecimal targetRatio, BigDecimal deviation) {
            this(executed, currentRatio, targetRatio, deviation, null);
        }
    }

    /**
     * 리밸런싱 상태 조회 (실행하지 않음)
     */
    public RebalanceStatus getStatus(String market) {
        boolean enabled = tradingProperties.getRebalancing().isEnabled();
        long cooldownMinutes = tradingProperties.getRebalancing().getCooldownMinutes();
        long cooldownRemainingSec = 0;
        if (lastRebalanceTime != null) {
            Duration elapsed = Duration.between(lastRebalanceTime, Instant.now());
            long remaining = cooldownMinutes * 60 - elapsed.getSeconds();
            cooldownRemainingSec = Math.max(0, remaining);
        }

        BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
        BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        if (krwAccount == null || coinAccount == null || currentPrice == null) {
            return new RebalanceStatus(enabled, null, null, null, null, null,
                    cooldownRemainingSec, lastRebalanceTime, "UNKNOWN", null, null, null);
        }

        BigDecimal krwBalance = new BigDecimal(krwAccount.balance());
        BigDecimal coinBalance = new BigDecimal(coinAccount.balance());
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal coinValue = coinBalance.multiply(currentPriceBD);
        BigDecimal totalValue = krwBalance.add(coinValue);

        BigDecimal currentRatio = null;
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            currentRatio = coinValue.divide(totalValue, 4, RoundingMode.HALF_UP);
        }

        String marketRegime;
        BigDecimal ma60 = null;
        BigDecimal targetRatio;

        if (isDailyRegimeSource()) {
            // DAILY: 목표 비중과 같은 판정을 보여준다 (ADR trading/risk/0007)
            MarketRegime regime = dailyRegimeService.currentRegime();
            marketRegime = regime.name();
            targetRatio = BigDecimal.valueOf(regime == MarketRegime.BULL
                    ? tradingProperties.getRebalancing().getBullRatio()
                    : regime == MarketRegime.BEAR
                    ? tradingProperties.getRebalancing().getBearRatio()
                    : tradingProperties.getRebalancing().getDefaultRatio());
            BigDecimal deviation = currentRatio != null ? currentRatio.subtract(targetRatio) : null;
            return new RebalanceStatus(enabled, currentRatio, targetRatio, deviation, krwBalance, coinBalance,
                    cooldownRemainingSec, lastRebalanceTime, marketRegime, currentPriceBD, null,
                    BigDecimal.valueOf(tradingProperties.getRebalancing().getDeviationTrigger()));
        }

        IndicatorResult indicators = indicatorService.calculate(market);
        ma60 = indicators != null ? indicators.ma60() : null;

        if (indicators == null || indicators.ma60() == null) {
            marketRegime = "INSUFFICIENT_DATA";
            targetRatio = BigDecimal.valueOf(tradingProperties.getRebalancing().getDefaultRatio());
        } else if (indicators.isPriceAboveMa60()) {
            marketRegime = "BULL";
            targetRatio = BigDecimal.valueOf(tradingProperties.getRebalancing().getBullRatio());
        } else if (indicators.isPriceBelowMa60()) {
            marketRegime = "BEAR";
            targetRatio = BigDecimal.valueOf(tradingProperties.getRebalancing().getBearRatio());
        } else {
            marketRegime = "NEUTRAL";
            targetRatio = BigDecimal.valueOf(tradingProperties.getRebalancing().getDefaultRatio());
        }

        BigDecimal deviation = null;
        if (currentRatio != null) {
            deviation = currentRatio.subtract(targetRatio);
        }

        return new RebalanceStatus(
                enabled,
                currentRatio,
                targetRatio,
                deviation,
                krwBalance,
                coinBalance,
                cooldownRemainingSec,
                lastRebalanceTime,
                marketRegime,
                currentPriceBD,
                ma60,
                BigDecimal.valueOf(tradingProperties.getRebalancing().getDeviationTrigger())
        );
    }

    public record RebalanceStatus(
        boolean enabled,
        BigDecimal currentRatio,
        BigDecimal targetRatio,
        BigDecimal deviation,
        BigDecimal krwBalance,
        BigDecimal coinBalance,
        long cooldownRemainingSec,
        Instant lastRebalanceTime,
        String marketRegime,
        BigDecimal currentPrice,
        BigDecimal ma60,
        BigDecimal deviationTrigger
    ) {}
}
