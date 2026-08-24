package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Closeable;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 갭 상승 눌림목 매매 봇 서비스 (Main Orchestrator)
 *
 * 거래 타임라인:
 * 08:30~09:00  사전 준비 (정적 유니버스 스냅샷 — 거래량순위 미호출)
 * 09:00~09:20  갭 상승 종목 스크리닝 (cron 09:20)
 * 09:20~11:20  눌림목 감지 및 진입/청산
 * 11:20~11:30  최종 청산
 */
@Service
@Transactional(readOnly = true)
public class GapPullbackBotService {

    private static final Logger log = LoggerFactory.getLogger(GapPullbackBotService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    /**
     * 보호 전용 복구 모드 — 재시작 시 오픈 포지션이 있어 자동 재개된 상태.
     * 리스크 루프·시간청산은 돌지만 신규 진입은 차단한다 (2026-07-24 리뷰 §6 / P1-5).
     */
    private final AtomicBoolean recoveryMode = new AtomicBoolean(false);

    private final ScreeningService screeningService;
    private final PullbackDetectionService pullbackDetectionService;
    private final StockPositionService positionService;
    private final StockRiskService riskService;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;
    private final StockMailService mailService;
    private final StockBotMetrics metrics;
    private final UniverseBuilder universeBuilder;
    private final Clock clock;

    private LocalDateTime startedAt;
    private LocalDate currentTradingDate;

    public GapPullbackBotService(ScreeningService screeningService,
                                  PullbackDetectionService pullbackDetectionService,
                                  StockPositionService positionService,
                                  StockRiskService riskService,
                                  KoreaInvestmentApiClient kisApiClient,
                                  StockProperties stockProperties,
                                  StockMailService mailService,
                                  StockBotMetrics metrics,
                                  UniverseBuilder universeBuilder,
                                  Clock clock) {
        this.screeningService = screeningService;
        this.pullbackDetectionService = pullbackDetectionService;
        this.positionService = positionService;
        this.riskService = riskService;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
        this.mailService = mailService;
        this.metrics = metrics;
        this.universeBuilder = universeBuilder;
        this.clock = clock;
    }

    /**
     * 기동 시 보호 전용 자동 재개 — 오픈 포지션이 있는데 봇이 멈춰 있으면 손절·트레일링·
     * 11:20 강제청산이 전부 죽는다. 리스크 루프만 살리고 신규 진입은 차단한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeProtectionOnStartup() {
        if (!stockProperties.getBot().isEnabled() || running.get()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        int openPositions = positionService.countOpenPositions(today);
        if (openPositions == 0) {
            return;
        }

        running.set(true);
        paused.set(false);
        recoveryMode.set(true);
        currentTradingDate = today;
        startedAt = LocalDateTime.now(clock);

        log.warn("오픈 포지션 {}건 발견 — 보호 전용 모드로 자동 재개 (리스크 관리·시간청산만, "
            + "신규 진입은 관리자 start() 필요)", openPositions);
        TradeEvents.event("RECOVERY_RESUMED")
            .with("tradingDate", today)
            .with("openPositions", openPositions)
            .log();
    }

    // ========== Bot Lifecycle ==========

    /**
     * 봇 시작
     */
    public boolean start() {
        if (running.get()) {
            log.warn("Bot is already running");
            return false;
        }

        if (!kisApiClient.isConfigured()) {
            log.error("KIS API not configured. Cannot start bot.");
            return false;
        }

        running.set(true);
        paused.set(false);
        recoveryMode.set(false); // 관리자가 명시적으로 시작 → 신규 진입까지 완전 재개
        startedAt = LocalDateTime.now(clock);
        currentTradingDate = LocalDate.now(clock);

        log.info("Gap & Pullback bot started at {}", startedAt);
        return true;
    }

    /**
     * 봇 정지
     */
    public boolean stop() {
        if (!running.get()) {
            log.warn("Bot is not running");
            return false;
        }

        running.set(false);
        paused.set(false);
        log.info("Gap & Pullback bot stopped");
        return true;
    }

    /**
     * 봇 일시정지
     */
    public boolean pause() {
        if (!running.get()) {
            log.warn("Bot is not running");
            return false;
        }

        paused.set(true);
        log.info("Gap & Pullback bot paused");
        return true;
    }

    /**
     * 봇 재개
     */
    public boolean resume() {
        if (!running.get()) {
            log.warn("Bot is not running");
            return false;
        }

        paused.set(false);
        log.info("Gap & Pullback bot resumed");
        return true;
    }

    /**
     * 봇 상태 조회
     */
    public BotStatus getStatus() {
        LocalDate today = LocalDate.now(clock);
        int watchingCount = 0;
        int positionCount = 0;

        if (running.get()) {
            List<Stock> activeStocks = screeningService.getActiveStocks(today);
            watchingCount = activeStocks.size();
            positionCount = positionService.countOpenPositions(today);
        }

        Instant lastTick = metrics.getLastTradingTickAt();
        StockBotMetrics.ScreeningSnapshot lastScreening = metrics.getLastScreeningResult();

        return new BotStatus(
            running.get(),
            paused.get(),
            recoveryMode.get(),
            watchingCount,
            positionCount,
            getTradingPhase(),
            lastTick,
            lastScreening,
            metrics.apiCallsLast5min(),
            startedAt
        );
    }

    /**
     * 현재 거래 단계 반환
     */
    private String getTradingPhase() {
        if (!running.get()) {
            return "STOPPED";
        }
        if (paused.get()) {
            return "PAUSED";
        }

        LocalTime now = LocalTime.now(clock);
        LocalTime preMarket = LocalTime.parse(stockProperties.getTrading().getPreMarketStart());
        LocalTime marketOpen = LocalTime.parse(stockProperties.getTrading().getMarketOpen());
        LocalTime screeningEnd = LocalTime.parse(stockProperties.getTrading().getTradingLoopStart());
        LocalTime finalExit = LocalTime.parse(stockProperties.getExit().getFinalExitTime());
        LocalTime tradingEnd = LocalTime.parse(stockProperties.getTrading().getTradingEnd());

        if (now.isBefore(preMarket)) {
            return "PRE_MARKET_WAIT";
        } else if (now.isBefore(marketOpen)) {
            return "PRE_MARKET";
        } else if (now.isBefore(screeningEnd)) {
            return "SCREENING";
        } else if (now.isBefore(finalExit)) {
            return "TRADING";
        } else if (now.isBefore(tradingEnd)) {
            return "FINAL_EXIT";
        } else {
            return "MARKET_CLOSED";
        }
    }

    // ========== Trading Loops ==========

    /**
     * 사전 준비 루프 (08:30~09:00)
     * - 정적 유니버스 스냅샷 (refreshStaticOnly — 거래량순위 미호출)
     * - 관심종목 풀 준비
     */
    @Transactional
    public void executePreMarketLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        log.info("Executing pre-market loop");
        currentTradingDate = LocalDate.now(clock);

        // 그날의 유니버스를 미리 빌드해 캐시. 이 시각엔 당일 거래량이 없어 거래량순위가
        // 구조적으로 0~1건이므로 정적 소스(pinned ∪ fallback)만 쓴다 — 동적 소스는 장중
        // 거래량이 쌓인 09:20 refreshIfDegraded 가 담당 (ADR stock/algorithm/0010).
        UniverseBuilder.Snapshot universe = universeBuilder.refreshStaticOnly(currentTradingDate);
        log.info("Pre-market universe size: {}", universe.codes().size());
    }

    /**
     * 스크리닝 루프 (cron 09:20)
     * - 갭 상승 종목 스크리닝
     */
    @Transactional
    public void executeScreeningLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        try (Closeable ignored = TradeEvents.tradingDate(currentTradingDate)) {
            log.info("Executing screening loop");

            // 08:30 스냅샷의 rank 가 요청한 top-N 에 미달하면 장중 거래량이 쌓인 지금 거래량순위를 재시도.
            UniverseBuilder.Snapshot universe = universeBuilder.refreshIfDegraded(currentTradingDate);
            List<String> stockCodes = universe.codes();
            if (stockCodes.isEmpty()) {
                log.warn("Universe is empty - no stocks to screen. Check stock.universe.* config.");
                TradeEvents.event("SCREENING_SKIPPED").with("reason", "empty_universe").log();
                return;
            }

            List<Stock> selectedStocks = screeningService.executeScreening(
                currentTradingDate, stockCodes);

            log.info("Screening complete: {} stocks selected", selectedStocks.size());
            TradeEvents.event("SCREENING_COMPLETED")
                .with("universe", stockCodes.size())
                .with("selected", selectedStocks.size())
                .log();

            try {
                mailService.sendScreeningResult(currentTradingDate, selectedStocks, universe);
            } catch (Exception e) {
                log.error("Failed to send screening result email: {}", e.getMessage());
            }
        } catch (java.io.IOException e) {
            log.error("Closeable failure (should not happen): {}", e.getMessage());
        }
    }

    /**
     * 메인 트레이딩 루프 (09:20~11:20)
     * - 리스크 관리
     * - 상태 머신 업데이트
     * - 진입 실행
     */
    @Transactional
    public void executeTradingLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        LocalTime now = LocalTime.now(clock);
        LocalTime screeningEnd = LocalTime.parse(stockProperties.getTrading().getTradingLoopStart());
        LocalTime finalExit = LocalTime.parse(stockProperties.getExit().getFinalExitTime());

        if (now.isBefore(screeningEnd) || now.isAfter(finalExit)) {
            return;
        }

        try (Closeable ignored = TradeEvents.tradingDate(currentTradingDate)) {
            metrics.recordTradingTick();
            log.debug("Executing trading loop at {}", now);

            // 0. 미확인 주문 스윕 (고아 체결 → 무보호 포지션 제거). 실패해도 리스크 체크는 진행.
            try {
                positionService.reconcileUnconfirmedOrders(currentTradingDate);
            } catch (Exception e) {
                log.warn("미확인 주문 스윕 실패: {}", e.getMessage());
            }

            // 1. 리스크 관리 (손절/익절/트레일링)
            riskService.checkAndExecuteRiskRules(currentTradingDate);

            // 2. 상태 머신 업데이트
            pullbackDetectionService.updateAllStockStates(currentTradingDate);

            // 3. 진입 실행 — 보호 전용 복구 모드에서는 신규 진입 차단
            if (recoveryMode.get()) {
                log.debug("복구 모드 — 신규 진입 스킵 (보호 로직만 동작)");
            } else {
                executeEntries();
            }
        } catch (java.io.IOException e) {
            log.error("Closeable failure (should not happen): {}", e.getMessage());
        }
    }

    /**
     * 진입 실행
     */
    private void executeEntries() {
        // 최대 포지션 수 체크
        int maxPositions = stockProperties.getBot().getMaxPositions();
        int currentPositions = positionService.countOpenPositions(currentTradingDate);

        if (currentPositions >= maxPositions) {
            log.debug("Max positions reached: {}/{}", currentPositions, maxPositions);
            return;
        }

        // 진입 준비 종목 조회
        List<Stock> entryReadyStocks = pullbackDetectionService.getEntryReadyStocks(currentTradingDate);

        for (Stock stock : entryReadyStocks) {
            if (currentPositions >= maxPositions) {
                break;
            }

            try (Closeable ignored = TradeEvents.stockCode(stock.getStockCode())) {
                StockPosition position = positionService.openPosition(stock);
                if (position != null) {
                    currentPositions++;
                    log.info("Position opened for {}", stock.getStockCode());
                    TradeEvents.event("POSITION_OPENED")
                        .with("stockCode", stock.getStockCode())
                        .with("entryPrice", position.getEntryPrice())
                        .with("quantity", position.getEntryQuantity())
                        .with("stopLoss", position.getStopLossPrice())
                        .log();
                } else {
                    TradeEvents.event("POSITION_OPEN_FAILED")
                        .with("stockCode", stock.getStockCode())
                        .log();
                }
            } catch (Exception e) {
                log.error("Error opening position for {}: {}", stock.getStockCode(), e.getMessage());
                TradeEvents.event("POSITION_OPEN_ERROR")
                    .with("stockCode", stock.getStockCode())
                    .with("error", e.getClass().getSimpleName())
                    .with("message", e.getMessage())
                    .warn();
            }
        }
    }

    /**
     * 최종 청산 체크 (11:20)
     */
    @Transactional
    public void executeFinalExitCheck() {
        if (!running.get()) {
            return;
        }

        log.warn("Executing final exit - closing all positions");
        riskService.executeTimeBasedExit(currentTradingDate);
    }

    /**
     * 긴급 청산
     */
    @Transactional
    public void emergencyCloseAll() {
        log.warn("EMERGENCY CLOSE requested");
        riskService.emergencyCloseAll(currentTradingDate);
    }

    // ========== Status Record ==========

    public record BotStatus(
        boolean running,
        boolean paused,
        boolean recoveryMode,
        int watchingCount,
        int positionCount,
        String tradingPhase,
        Instant lastTradingTickAt,
        StockBotMetrics.ScreeningSnapshot lastScreeningResult,
        int apiCallsLast5min,
        LocalDateTime startedAt
    ) {}
}
