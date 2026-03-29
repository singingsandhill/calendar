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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
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
     * 다단계 익절 체크 (수수료 포함 최소 수익률 검증)
     * 시간 감소 익절: 장 후반으로 갈수록 minProfitThreshold 감소
     */
    private void checkTakeProfitLevels(StockPosition position, BigDecimal currentPrice) {
        BigDecimal tp1Percent = stockProperties.getExit().getTp1Percent();
        BigDecimal tp3Percent = stockProperties.getExit().getTp3Percent();
        BigDecimal commissionRate = stockProperties.getRisk().getCommissionRate();
        BigDecimal sellTaxRate = stockProperties.getRisk().getSellTaxRate();
        BigDecimal minProfitThreshold = calculateTimeDecayThreshold();
        // minProfitThreshold는 비율(0.005=0.5%)이므로 % 단위로 변환
        BigDecimal minProfitPct = minProfitThreshold.multiply(new BigDecimal("100"));

        // TP1: +1.5% → 50% 매도
        if (position.shouldTp1(currentPrice, tp1Percent)) {
            BigDecimal pnlPctWithFee = position.calculateUnrealizedPnlPctWithFee(
                currentPrice, commissionRate, sellTaxRate);
            if (pnlPctWithFee.compareTo(minProfitPct) < 0) {
                log.debug("[{}] TP1 보류: 수수료 포함 수익률={}% < 최소 {}%",
                    position.getStockCode(), pnlPctWithFee, minProfitPct);
                return;
            }
            int quantity = position.calculateTp1Quantity();
            if (quantity > 0) {
                log.info("TP1 triggered for {}: {} shares @ {} (수수료 포함 수익률={}%)",
                    position.getStockCode(), quantity, currentPrice, pnlPctWithFee);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP1);
            }
        }

        // TP2: 당일 고점 → 잔여의 60% 매도
        else if (position.shouldTp2(currentPrice)) {
            BigDecimal pnlPctWithFee = position.calculateUnrealizedPnlPctWithFee(
                currentPrice, commissionRate, sellTaxRate);
            if (pnlPctWithFee.compareTo(minProfitPct) < 0) {
                log.debug("[{}] TP2 보류: 수수료 포함 수익률={}% < 최소 {}%",
                    position.getStockCode(), pnlPctWithFee, minProfitPct);
                return;
            }
            int quantity = position.calculateTp2Quantity();
            if (quantity > 0) {
                log.info("TP2 triggered for {}: {} shares @ {} (수수료 포함 수익률={}%)",
                    position.getStockCode(), quantity, currentPrice, pnlPctWithFee);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP2);
            }
        }

        // TP3: 고점 +1% → 잔량 전량 매도
        else if (position.shouldTp3(currentPrice, tp3Percent)) {
            BigDecimal pnlPctWithFee = position.calculateUnrealizedPnlPctWithFee(
                currentPrice, commissionRate, sellTaxRate);
            if (pnlPctWithFee.compareTo(minProfitPct) < 0) {
                log.debug("[{}] TP3 보류: 수수료 포함 수익률={}% < 최소 {}%",
                    position.getStockCode(), pnlPctWithFee, minProfitPct);
                return;
            }
            int quantity = position.calculateTp3Quantity();
            if (quantity > 0) {
                log.info("TP3 triggered for {}: {} shares @ {} (수수료 포함 수익률={}%)",
                    position.getStockCode(), quantity, currentPrice, pnlPctWithFee);
                positionService.executePartialExit(position, quantity, currentPrice, StockCloseReason.TP3);
            }
        }
    }

    /**
     * 시간 기반 최소 수익률 임계값 계산
     * 09:10 → minProfitThreshold (0.5%)
     * 15:15 → minProfitThresholdLate (0.1%) 또는 0%
     * 선형 감소
     */
    private BigDecimal calculateTimeDecayThreshold() {
        StockProperties.Risk riskConfig = stockProperties.getRisk();

        if (!riskConfig.isTimeDecayEnabled()) {
            return riskConfig.getMinProfitThreshold();
        }

        LocalTime now = LocalTime.now();
        LocalTime tradingStart = LocalTime.of(9, 10);
        LocalTime tradingEnd = LocalTime.of(15, 15);

        if (now.isBefore(tradingStart)) {
            return riskConfig.getMinProfitThreshold();
        }
        if (!now.isBefore(tradingEnd)) {
            return BigDecimal.ZERO;
        }

        BigDecimal earlyThreshold = riskConfig.getMinProfitThreshold();
        BigDecimal lateThreshold = riskConfig.getMinProfitThresholdLate();

        long totalSeconds = java.time.Duration.between(tradingStart, tradingEnd).getSeconds();
        long elapsedSeconds = java.time.Duration.between(tradingStart, now).getSeconds();

        BigDecimal progress = BigDecimal.valueOf(elapsedSeconds)
            .divide(BigDecimal.valueOf(totalSeconds), 6, RoundingMode.HALF_UP);

        BigDecimal decayed = earlyThreshold.subtract(
            earlyThreshold.subtract(lateThreshold).multiply(progress));

        log.debug("Time-decay threshold: {} (time={}, progress={}%)",
            decayed.setScale(4, RoundingMode.HALF_UP), now,
            progress.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));

        return decayed;
    }

    /**
     * 트레일링 스탑 업데이트 (손익분기점 보장)
     */
    @Transactional
    public void updateTrailingStop(StockPosition position, BigDecimal currentPrice) {
        BigDecimal trailingPercent = stockProperties.getRisk().getTrailingStopPercent();
        BigDecimal roundTripFeeRate = stockProperties.getRisk().getRoundTripFeeRate();

        // 손익분기점 = 진입가 × (1 + 왕복수수료)
        BigDecimal breakEvenPrice = position.getEntryPrice()
            .multiply(BigDecimal.ONE.add(roundTripFeeRate))
            .setScale(0, java.math.RoundingMode.UP);

        position.updateTrailingStop(currentPrice, trailingPercent, breakEvenPrice);
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
