package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 08:30~09:00  사전 스크리닝 (전일 데이터 수집)
 * 09:00~09:10  갭 상승 종목 스크리닝
 * 09:10~11:20  눌림목 감지 및 진입/청산
 * 11:20~11:30  최종 청산
 */
@Service
@Transactional(readOnly = true)
public class GapPullbackBotService {

    private static final Logger log = LoggerFactory.getLogger(GapPullbackBotService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final ScreeningService screeningService;
    private final PullbackDetectionService pullbackDetectionService;
    private final StockPositionService positionService;
    private final StockRiskService riskService;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;
    private final StockMailService mailService;

    private LocalDateTime startedAt;
    private LocalDate currentTradingDate;

    public GapPullbackBotService(ScreeningService screeningService,
                                  PullbackDetectionService pullbackDetectionService,
                                  StockPositionService positionService,
                                  StockRiskService riskService,
                                  KoreaInvestmentApiClient kisApiClient,
                                  StockProperties stockProperties,
                                  StockMailService mailService) {
        this.screeningService = screeningService;
        this.pullbackDetectionService = pullbackDetectionService;
        this.positionService = positionService;
        this.riskService = riskService;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
        this.mailService = mailService;
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
        startedAt = LocalDateTime.now();
        currentTradingDate = LocalDate.now(KST);

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
        LocalDate today = LocalDate.now(KST);
        int watchingCount = 0;
        int positionCount = 0;

        if (running.get()) {
            List<Stock> activeStocks = screeningService.getActiveStocks(today);
            watchingCount = activeStocks.size();
            positionCount = positionService.countOpenPositions(today);
        }

        return new BotStatus(
            running.get(),
            paused.get(),
            watchingCount,
            positionCount,
            getTradingPhase(),
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

        LocalTime now = LocalTime.now(KST);
        LocalTime preMarket = LocalTime.parse(stockProperties.getTrading().getPreMarketStart());
        LocalTime marketOpen = LocalTime.parse(stockProperties.getTrading().getMarketOpen());
        LocalTime screeningEnd = LocalTime.parse(stockProperties.getTrading().getScreeningEnd());
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
     * - 전일 데이터 수집
     * - 관심종목 풀 준비
     */
    @Transactional
    public void executePreMarketLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        log.info("Executing pre-market loop");
        currentTradingDate = LocalDate.now(KST);

        // TODO: 전일 데이터 수집 로직
        // 현재는 스크리닝 단계에서 실시간 데이터 사용
    }

    /**
     * 스크리닝 루프 (09:00~09:10)
     * - 갭 상승 종목 스크리닝
     */
    @Transactional
    public void executeScreeningLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        log.info("Executing screening loop");

        // TODO: 전체 종목 목록 가져오기 (외부 데이터 필요)
        // 현재는 샘플 종목 코드로 테스트
        List<String> stockCodes = getSampleStockCodes();

        List<Stock> selectedStocks = screeningService.executeScreening(
            currentTradingDate, stockCodes);

        log.info("Screening complete: {} stocks selected", selectedStocks.size());

        // 이메일 발송
        try {
            mailService.sendScreeningResult(currentTradingDate, selectedStocks);
        } catch (Exception e) {
            log.error("Failed to send screening result email: {}", e.getMessage());
        }
    }

    /**
     * 샘플 종목 코드 (테스트용)
     */
    private List<String> getSampleStockCodes() {
        // 코스피 대형주 일부
        return List.of(
            "005930", // 삼성전자
            "000660", // SK하이닉스
            "035420", // NAVER
            "035720", // 카카오
            "051910", // LG화학
            "006400", // 삼성SDI
            "068270", // 셀트리온
            "028260", // 삼성물산
            "105560", // KB금융
            "055550"  // 신한지주
        );
    }

    /**
     * 메인 트레이딩 루프 (09:10~11:20)
     * - 리스크 관리
     * - 상태 머신 업데이트
     * - 진입 실행
     */
    @Transactional
    public void executeTradingLoop() {
        if (!running.get() || paused.get()) {
            return;
        }

        LocalTime now = LocalTime.now(KST);
        LocalTime screeningEnd = LocalTime.parse(stockProperties.getTrading().getScreeningEnd());
        LocalTime finalExit = LocalTime.parse(stockProperties.getExit().getFinalExitTime());

        // 거래 시간 체크
        if (now.isBefore(screeningEnd) || now.isAfter(finalExit)) {
            return;
        }

        log.debug("Executing trading loop at {}", now);

        // 1. 리스크 관리 (손절/익절/트레일링)
        riskService.checkAndExecuteRiskRules(currentTradingDate);

        // 2. 상태 머신 업데이트
        pullbackDetectionService.updateAllStockStates(currentTradingDate);

        // 3. 진입 실행
        executeEntries();
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

            try {
                StockPosition position = positionService.openPosition(stock);
                if (position != null) {
                    currentPositions++;
                    log.info("Position opened for {}", stock.getStockCode());
                }
            } catch (Exception e) {
                log.error("Error opening position for {}: {}", stock.getStockCode(), e.getMessage());
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
        int watchingCount,
        int positionCount,
        String tradingPhase,
        LocalDateTime startedAt
    ) {}
}
