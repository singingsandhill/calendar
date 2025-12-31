package me.singingsandhill.calendar.stock.domain.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 포지션 엔티티
 * - 다단계 익절 지원 (TP1, TP2, TP3)
 * - 트레일링 스탑 관리
 */
public class StockPosition {

    private Long id;
    private Long stockId;
    private final String stockCode;
    private final LocalDate tradingDate;
    private StockPositionStatus status;

    // 진입 정보
    private BigDecimal entryPrice;
    private Integer entryQuantity;
    private BigDecimal entryAmount;
    private LocalDateTime enteredAt;

    // 현재 포지션
    private Integer remainingQuantity;
    private BigDecimal averageExitPrice;

    // 다단계 익절 추적
    private boolean tp1Executed;
    private boolean tp2Executed;
    private boolean tp3Executed;
    private BigDecimal dayHighPrice;

    // 손절/트레일링
    private BigDecimal stopLossPrice;
    private BigDecimal trailingHigh;
    private BigDecimal trailingStopPrice;
    private boolean trailingActive;

    // 손익
    private BigDecimal realizedPnl;
    private BigDecimal realizedPnlPercent;
    private BigDecimal totalExitAmount;
    private Integer totalExitQuantity;

    private StockCloseReason closeReason;
    private LocalDateTime closedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StockPosition(String stockCode, LocalDate tradingDate) {
        this.stockCode = stockCode;
        this.tradingDate = tradingDate;
        this.status = StockPositionStatus.OPEN;
        this.tp1Executed = false;
        this.tp2Executed = false;
        this.tp3Executed = false;
        this.trailingActive = false;
        this.realizedPnl = BigDecimal.ZERO;
        this.totalExitAmount = BigDecimal.ZERO;
        this.totalExitQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 새 포지션 오픈
     */
    public static StockPosition open(String stockCode, LocalDate tradingDate,
                                      BigDecimal entryPrice, Integer quantity,
                                      BigDecimal stopLossPrice, BigDecimal dayHighPrice) {
        StockPosition position = new StockPosition(stockCode, tradingDate);
        position.entryPrice = entryPrice;
        position.entryQuantity = quantity;
        position.entryAmount = entryPrice.multiply(BigDecimal.valueOf(quantity));
        position.remainingQuantity = quantity;
        position.stopLossPrice = stopLossPrice;
        position.dayHighPrice = dayHighPrice;
        position.enteredAt = LocalDateTime.now();
        return position;
    }

    // ========== 손익 계산 ==========

    /**
     * 미실현 손익 계산
     */
    public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
        if (remainingQuantity == null || remainingQuantity == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        return currentValue.subtract(costBasis);
    }

    /**
     * 미실현 손익률 계산
     */
    public BigDecimal calculateUnrealizedPnlPercent(BigDecimal currentPrice) {
        if (entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(entryPrice)
            .multiply(new BigDecimal("100"))
            .divide(entryPrice, 4, RoundingMode.HALF_UP);
    }

    // ========== 손절/익절 체크 ==========

    /**
     * 손절 조건 체크 (-1.5% from entry)
     */
    public boolean shouldStopLoss(BigDecimal currentPrice) {
        if (stopLossPrice == null) {
            return false;
        }
        return currentPrice.compareTo(stopLossPrice) <= 0;
    }

    /**
     * 1차 익절 조건 체크 (+1.5% from entry)
     */
    public boolean shouldTp1(BigDecimal currentPrice, BigDecimal tp1Percent) {
        if (tp1Executed) {
            return false;
        }
        BigDecimal targetPrice = entryPrice.multiply(
            BigDecimal.ONE.add(tp1Percent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );
        return currentPrice.compareTo(targetPrice) >= 0;
    }

    /**
     * 2차 익절 조건 체크 (당일 고점)
     */
    public boolean shouldTp2(BigDecimal currentPrice) {
        if (tp2Executed || !tp1Executed || dayHighPrice == null) {
            return false;
        }
        return currentPrice.compareTo(dayHighPrice) >= 0;
    }

    /**
     * 3차 익절 조건 체크 (고점 +1%)
     */
    public boolean shouldTp3(BigDecimal currentPrice, BigDecimal tp3Percent) {
        if (tp3Executed || !tp2Executed || dayHighPrice == null) {
            return false;
        }
        BigDecimal targetPrice = dayHighPrice.multiply(
            BigDecimal.ONE.add(tp3Percent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );
        return currentPrice.compareTo(targetPrice) >= 0;
    }

    /**
     * 트레일링 스탑 조건 체크
     */
    public boolean shouldTrailingStop(BigDecimal currentPrice) {
        if (!trailingActive || trailingStopPrice == null) {
            return false;
        }
        return currentPrice.compareTo(trailingStopPrice) <= 0;
    }

    // ========== 익절 수량 계산 ==========

    /**
     * 1차 익절 수량 (진입수량의 50%)
     */
    public int calculateTp1Quantity() {
        return (int) Math.floor(entryQuantity * 0.5);
    }

    /**
     * 2차 익절 수량 (잔여수량의 60%)
     */
    public int calculateTp2Quantity() {
        return (int) Math.floor(remainingQuantity * 0.6);
    }

    /**
     * 3차 익절 수량 (잔여 전량)
     */
    public int calculateTp3Quantity() {
        return remainingQuantity;
    }

    // ========== 포지션 업데이트 ==========

    /**
     * 부분 청산 실행
     */
    public void executePartialExit(int quantity, BigDecimal exitPrice, StockCloseReason reason) {
        if (quantity > remainingQuantity) {
            throw new IllegalArgumentException("Exit quantity exceeds remaining quantity");
        }

        BigDecimal exitAmount = exitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal pnl = exitAmount.subtract(costBasis);

        this.totalExitAmount = this.totalExitAmount.add(exitAmount);
        this.totalExitQuantity = this.totalExitQuantity + quantity;
        this.remainingQuantity = this.remainingQuantity - quantity;
        this.realizedPnl = this.realizedPnl.add(pnl);

        // 평균 청산가 계산
        this.averageExitPrice = this.totalExitAmount.divide(
            BigDecimal.valueOf(this.totalExitQuantity), 2, RoundingMode.HALF_UP);

        // 익절 플래그 업데이트
        switch (reason) {
            case TP1 -> this.tp1Executed = true;
            case TP2 -> this.tp2Executed = true;
            case TP3 -> this.tp3Executed = true;
            default -> {}
        }

        // 상태 업데이트
        if (this.remainingQuantity == 0) {
            this.status = StockPositionStatus.CLOSED;
            this.closeReason = reason;
            this.closedAt = LocalDateTime.now();
        } else {
            this.status = StockPositionStatus.PARTIAL;
        }

        calculateRealizedPnlPercent();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔여 전량 청산
     */
    public void closeRemaining(BigDecimal exitPrice, StockCloseReason reason) {
        if (remainingQuantity == null || remainingQuantity == 0) {
            return;
        }
        executePartialExit(remainingQuantity, exitPrice, reason);
    }

    /**
     * 실현손익률 계산
     */
    private void calculateRealizedPnlPercent() {
        BigDecimal totalCost = entryPrice.multiply(BigDecimal.valueOf(totalExitQuantity));
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            this.realizedPnlPercent = BigDecimal.ZERO;
        } else {
            this.realizedPnlPercent = realizedPnl
                .multiply(new BigDecimal("100"))
                .divide(totalCost, 4, RoundingMode.HALF_UP);
        }
    }

    /**
     * 트레일링 스탑 업데이트
     */
    public void updateTrailingStop(BigDecimal currentPrice, BigDecimal trailingPercent) {
        // 1차 익절 후 트레일링 활성화
        if (!trailingActive && tp1Executed) {
            this.trailingActive = true;
            this.trailingHigh = currentPrice;
        }

        if (!trailingActive) {
            return;
        }

        // 고점 갱신
        if (currentPrice.compareTo(trailingHigh) > 0) {
            this.trailingHigh = currentPrice;
            // 트레일링 스탑 가격 업데이트 (고점 대비 -0.8%)
            this.trailingStopPrice = trailingHigh.multiply(
                BigDecimal.ONE.subtract(trailingPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            );
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 당일 고점 업데이트
     */
    public void updateDayHighPrice(BigDecimal highPrice) {
        if (this.dayHighPrice == null || highPrice.compareTo(this.dayHighPrice) > 0) {
            this.dayHighPrice = highPrice;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // ========== Status Checks ==========

    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isClosed() {
        return status == StockPositionStatus.CLOSED;
    }

    public boolean hasRemainingQuantity() {
        return remainingQuantity != null && remainingQuantity > 0;
    }

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockCode() { return stockCode; }
    public LocalDate getTradingDate() { return tradingDate; }
    public StockPositionStatus getStatus() { return status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public Integer getEntryQuantity() { return entryQuantity; }
    public BigDecimal getEntryAmount() { return entryAmount; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public BigDecimal getAverageExitPrice() { return averageExitPrice; }
    public boolean isTp1Executed() { return tp1Executed; }
    public boolean isTp2Executed() { return tp2Executed; }
    public boolean isTp3Executed() { return tp3Executed; }
    public BigDecimal getDayHighPrice() { return dayHighPrice; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public BigDecimal getTrailingHigh() { return trailingHigh; }
    public BigDecimal getTrailingStopPrice() { return trailingStopPrice; }
    public boolean isTrailingActive() { return trailingActive; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getRealizedPnlPercent() { return realizedPnlPercent; }
    public StockCloseReason getCloseReason() { return closeReason; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
