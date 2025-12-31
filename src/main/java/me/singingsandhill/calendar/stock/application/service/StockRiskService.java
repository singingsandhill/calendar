package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 주식 리스크 관리 서비스
 * - 손절 (Stop Loss)
 * - 익절 (Take Profit - TP1, TP2, TP3)
 * - 트레일링 스탑
 * - 시간 기반 청산
 */
@Service
@Transactional(readOnly = true)
public class StockRiskService {

    private static final Logger log = LoggerFactory.getLogger(StockRiskService.class);

    private final StockPositionRepository positionRepository;
    private final StockPositionService positionService;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;

    public StockRiskService(StockPositionRepository positionRepository,
                            StockPositionService positionService,
                            KoreaInvestmentApiClient kisApiClient,
                            StockProperties stockProperties) {
        this.positionRepository = positionRepository;
        this.positionService = positionService;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
    }

    /**
     * 모든 오픈 포지션에 대해 리스크 체크 실행
     */
    @Transactional
    public void checkAndExecuteRiskRules(LocalDate tradingDate) {
        List<StockPosition> openPositions = positionRepository.findOpenPositions(tradingDate);

        for (StockPosition position : openPositions) {
            try {
                checkPositionRisk(position);
            } catch (Exception e) {
                log.error("Error checking risk for position {}: {}",
                    position.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * 단일 포지션 리스크 체크
     */
    @Transactional
    public void checkPositionRisk(StockPosition position) {
        String stockCode = position.getStockCode();

        // 현재가 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            return;
        }

        BigDecimal currentPrice = quote.currentPrice();

        // 1. 손절 체크 (-1.5%)
        if (position.shouldStopLoss(currentPrice)) {
            log.warn("STOP_LOSS triggered for {}: current={}, stopLoss={}",
                stockCode, currentPrice, position.getStopLossPrice());
            positionService.closePosition(position, currentPrice, StockCloseReason.STOP_LOSS);
            return;
        }

        // 2. 트레일링 스탑 체크
        if (position.shouldTrailingStop(currentPrice)) {
            log.warn("TRAILING_STOP triggered for {}: current={}, trailingStop={}",
                stockCode, currentPrice, position.getTrailingStopPrice());
            positionService.closePosition(position, currentPrice, StockCloseReason.TRAILING_STOP);
            return;
        }

        // 3. 익절 체크
        checkTakeProfitLevels(position, currentPrice);

        // 4. 트레일링 스탑 업데이트
        updateTrailingStop(position, currentPrice);

        // 5. 당일 고점 업데이트
        position.updateDayHighPrice(quote.highPrice());
        positionRepository.save(position);
    }

    /**
     * 다단계 익절 체크
     */
    private void checkTakeProfitLevels(StockPosition position, BigDecimal currentPrice) {
        BigDecimal tp1Percent = stockProperties.getExit().getTp1Percent();
        BigDecimal tp3Percent = stockProperties.getExit().getTp3Percent();

        // TP1: +1.5% → 50% 매도
        if (position.shouldTp1(currentPrice, tp1Percent)) {
            int quantity = position.calculateTp1Quantity();
            if (quantity > 0) {
                log.info("TP1 triggered for {}: {} shares @ {}",
                    position.getStockCode(), quantity, currentPrice);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP1);
            }
        }

        // TP2: 당일 고점 → 잔여의 60% 매도
        else if (position.shouldTp2(currentPrice)) {
            int quantity = position.calculateTp2Quantity();
            if (quantity > 0) {
                log.info("TP2 triggered for {}: {} shares @ {}",
                    position.getStockCode(), quantity, currentPrice);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP2);
            }
        }

        // TP3: 고점 +1% → 잔량 전량 매도
        else if (position.shouldTp3(currentPrice, tp3Percent)) {
            int quantity = position.calculateTp3Quantity();
            if (quantity > 0) {
                log.info("TP3 triggered for {}: {} shares @ {}",
                    position.getStockCode(), quantity, currentPrice);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP3);
            }
        }
    }

    /**
     * 트레일링 스탑 업데이트
     */
    @Transactional
    public void updateTrailingStop(StockPosition position, BigDecimal currentPrice) {
        BigDecimal trailingPercent = stockProperties.getRisk().getTrailingStopPercent();
        position.updateTrailingStop(currentPrice, trailingPercent);
        positionRepository.save(position);
    }

    /**
     * 시간 기반 청산 (11:20)
     */
    @Transactional
    public void executeTimeBasedExit(LocalDate tradingDate) {
        log.warn("Executing time-based exit for all open positions");

        List<StockPosition> openPositions = positionRepository.findOpenPositions(tradingDate);

        for (StockPosition position : openPositions) {
            try {
                KisQuoteResponse quote = kisApiClient.getQuote(position.getStockCode());
                if (quote != null) {
                    positionService.closePosition(position, quote.currentPrice(), StockCloseReason.TIME_EXIT);
                }
            } catch (Exception e) {
                log.error("Error closing position {} for time exit: {}",
                    position.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * 긴급 청산 (모든 포지션 즉시 청산)
     */
    @Transactional
    public void emergencyCloseAll(LocalDate tradingDate) {
        log.warn("EMERGENCY CLOSE: Closing all open positions");

        List<StockPosition> openPositions = positionRepository.findOpenPositions(tradingDate);

        for (StockPosition position : openPositions) {
            try {
                KisQuoteResponse quote = kisApiClient.getQuote(position.getStockCode());
                if (quote != null) {
                    positionService.closePosition(position, quote.currentPrice(), StockCloseReason.EMERGENCY);
                }
            } catch (Exception e) {
                log.error("Error during emergency close for {}: {}",
                    position.getStockCode(), e.getMessage());
            }
        }
    }
}
