package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderbookResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 눌림목 패턴 감지 서비스
 * 상태 머신: WATCHING → HIGH_FORMED → PULLBACK → ENTRY_READY
 */
@Service
@Transactional(readOnly = true)
public class PullbackDetectionService {

    private static final Logger log = LoggerFactory.getLogger(PullbackDetectionService.class);

    private final StockRepository stockRepository;
    private final StockSignalRepository signalRepository;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;

    public PullbackDetectionService(StockRepository stockRepository,
                                     StockSignalRepository signalRepository,
                                     KoreaInvestmentApiClient kisApiClient,
                                     StockProperties stockProperties) {
        this.stockRepository = stockRepository;
        this.signalRepository = signalRepository;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
    }

    /**
     * 모든 활성 종목의 상태 업데이트
     */
    @Transactional
    public void updateAllStockStates(LocalDate tradingDate) {
        List<Stock> activeStocks = stockRepository.findActiveStocks(tradingDate);

        for (Stock stock : activeStocks) {
            try {
                updateStockState(stock);
            } catch (Exception e) {
                log.warn("Error updating state for {}: {}", stock.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * 단일 종목 상태 업데이트
     */
    @Transactional
    public void updateStockState(Stock stock) {
        // 현재가 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stock.getStockCode());
        if (quote == null) {
            return;
        }

        BigDecimal currentPrice = quote.currentPrice();
        stock.updateCurrentPrice(currentPrice);

        StockState currentState = stock.getState();

        switch (currentState) {
            case WATCHING -> checkHighFormation(stock, currentPrice);
            case HIGH_FORMED -> checkPullbackEntry(stock, currentPrice);
            case PULLBACK -> checkBounceConfirmation(stock, currentPrice);
            default -> {}
        }

        stockRepository.save(stock);
    }

    /**
     * 고점 형성 체크 (WATCHING → HIGH_FORMED)
     * 조건: 현재가 >= 시가 × 1.015 (시가 대비 +1.5% 이상)
     */
    private void checkHighFormation(Stock stock, BigDecimal currentPrice) {
        BigDecimal threshold = stockProperties.getEntry().getHighThresholdPercent();

        if (stock.isHighFormed(threshold)) {
            stock.recordHighFormed(currentPrice);
            log.info("HIGH_FORMED: {} at {} (+{}% from open)",
                stock.getStockCode(), currentPrice, stock.calculateReturnFromOpen());

            // 고점 형성 시그널 저장
            StockSignal signal = StockSignal.highFormed(
                stock.getStockCode(), currentPrice, currentPrice);
            signalRepository.save(signal);
        }
    }

    /**
     * 눌림목 진입 체크 (HIGH_FORMED → PULLBACK)
     * 조건: 고점 대비 -1.5% ~ -3.0% 하락
     */
    private void checkPullbackEntry(Stock stock, BigDecimal currentPrice) {
        BigDecimal minPullback = stockProperties.getEntry().getPullbackMinPercent();
        BigDecimal maxPullback = stockProperties.getEntry().getPullbackMaxPercent();

        // 과도한 하락 체크 (-3.0% 초과)
        if (stock.isPullbackTooDeep(maxPullback)) {
            stock.markFilteredOut();
            log.info("FILTERED_OUT: {} dropped too deep ({}% from high)",
                stock.getStockCode(), stock.calculateDropFromHigh());
            return;
        }

        // 눌림목 범위 내 체크
        if (stock.isInPullbackRange(minPullback, maxPullback)) {
            stock.recordPullbackStart(currentPrice);
            log.info("PULLBACK: {} at {} ({}% from high)",
                stock.getStockCode(), currentPrice, stock.calculateDropFromHigh());
        }
    }

    /**
     * 반등 확인 체크 (PULLBACK → ENTRY_READY)
     * 조건: 눌림목 저점 대비 +0.3% 반등
     */
    private void checkBounceConfirmation(Stock stock, BigDecimal currentPrice) {
        BigDecimal maxPullback = stockProperties.getEntry().getPullbackMaxPercent();

        // 과도한 하락 재체크
        if (stock.isPullbackTooDeep(maxPullback)) {
            stock.markFilteredOut();
            log.info("FILTERED_OUT: {} dropped too deep during pullback ({}% from high)",
                stock.getStockCode(), stock.calculateDropFromHigh());
            return;
        }

        // 눌림목 저점 갱신
        stock.updatePullbackLow(currentPrice);

        // 반등 확인
        BigDecimal bounceThreshold = stockProperties.getEntry().getBounceThresholdPercent();
        if (stock.isBounceConfirmed(bounceThreshold)) {
            // 추가 조건 체크: 체결강도, 호가 불균형
            if (validateEntryConditions(stock)) {
                stock.markEntryReady();
                log.info("ENTRY_READY: {} at {} (+{}% bounce from pullback low)",
                    stock.getStockCode(), currentPrice, stock.calculateBounceFromLow());

                // 진입 준비 시그널 저장
                StockSignal signal = StockSignal.pullbackEntry(
                    stock.getStockCode(),
                    stock.getHighAfterOpen(),
                    stock.calculateDropFromHigh(),
                    stock.calculateBounceFromLow(),
                    currentPrice
                );
                signalRepository.save(signal);
            }
        }
    }

    /**
     * 진입 조건 추가 검증
     * - 체결강도 >= 105
     * - 호가 불균형 (매수잔량 / 매도잔량) > 1.2
     */
    private boolean validateEntryConditions(Stock stock) {
        // 체결강도 체크
        BigDecimal tradeStrength = kisApiClient.getTradeStrength(stock.getStockCode());
        if (tradeStrength != null && tradeStrength.compareTo(new BigDecimal("105")) < 0) {
            log.debug("Entry rejected for {}: weak trade strength {}",
                stock.getStockCode(), tradeStrength);
            return false;
        }

        // 호가 불균형 체크
        KisOrderbookResponse orderbook = kisApiClient.getOrderbook(stock.getStockCode());
        if (orderbook != null) {
            BigDecimal imbalance = orderbook.calculateOrderImbalance();
            if (imbalance.compareTo(new BigDecimal("1.2")) < 0) {
                log.debug("Entry rejected for {}: weak order imbalance {}",
                    stock.getStockCode(), imbalance);
                return false;
            }
        }

        // 눌림목 시간 체크 (최소 3분, 최대 15분)
        LocalDateTime pullbackStart = stock.getPullbackStartAt();
        if (pullbackStart != null) {
            long pullbackMinutes = java.time.Duration.between(pullbackStart, LocalDateTime.now()).toMinutes();
            int minMinutes = stockProperties.getEntry().getMinPullbackMinutes();
            int maxMinutes = stockProperties.getEntry().getMaxPullbackMinutes();

            if (pullbackMinutes < minMinutes) {
                log.debug("Entry rejected for {}: pullback too short {}min",
                    stock.getStockCode(), pullbackMinutes);
                return false;
            }
            if (pullbackMinutes > maxMinutes) {
                log.debug("Entry rejected for {}: pullback too long {}min",
                    stock.getStockCode(), pullbackMinutes);
                return false;
            }
        }

        return true;
    }

    /**
     * 진입 준비 완료된 종목 조회
     */
    public List<Stock> getEntryReadyStocks(LocalDate tradingDate) {
        return stockRepository.findByTradingDateAndState(tradingDate, StockState.ENTRY_READY);
    }

    /**
     * 종목 필터 아웃 처리
     */
    @Transactional
    public void filterOutStock(Stock stock) {
        stock.markFilteredOut();
        stockRepository.save(stock);
    }
}
