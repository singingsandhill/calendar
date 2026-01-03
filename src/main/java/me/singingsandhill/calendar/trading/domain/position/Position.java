package me.singingsandhill.calendar.trading.domain.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Position {

    private Long id;
    private final String market;
    private PositionStatus status;
    private BigDecimal entryPrice;
    private BigDecimal entryVolume;
    private BigDecimal entryAmount;
    private BigDecimal exitPrice;
    private BigDecimal exitVolume;
    private BigDecimal exitAmount;
    private BigDecimal realizedPnl;
    private BigDecimal realizedPnlPct;
    private BigDecimal stopLossPrice;
    private BigDecimal takeProfitPrice;
    private BigDecimal trailingStopPrice;
    private BigDecimal highWaterMark;
    private boolean trailingStopActive;
    private CloseReason closeReason;
    private final LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private final LocalDateTime createdAt;
    private BigDecimal entryFee;
    private BigDecimal exitFee;
    private BigDecimal totalFees;

    public Position(Long id, String market, PositionStatus status,
                    BigDecimal entryPrice, BigDecimal entryVolume, BigDecimal entryAmount,
                    BigDecimal exitPrice, BigDecimal exitVolume, BigDecimal exitAmount,
                    BigDecimal realizedPnl, BigDecimal realizedPnlPct,
                    BigDecimal stopLossPrice, BigDecimal takeProfitPrice,
                    BigDecimal trailingStopPrice, BigDecimal highWaterMark, boolean trailingStopActive,
                    CloseReason closeReason, LocalDateTime openedAt, LocalDateTime closedAt,
                    LocalDateTime createdAt, BigDecimal entryFee, BigDecimal exitFee, BigDecimal totalFees) {
        this.id = id;
        this.market = market;
        this.status = status;
        this.entryPrice = entryPrice;
        this.entryVolume = entryVolume;
        this.entryAmount = entryAmount;
        this.exitPrice = exitPrice;
        this.exitVolume = exitVolume;
        this.exitAmount = exitAmount;
        this.realizedPnl = realizedPnl;
        this.realizedPnlPct = realizedPnlPct;
        this.stopLossPrice = stopLossPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.trailingStopPrice = trailingStopPrice;
        this.highWaterMark = highWaterMark;
        this.trailingStopActive = trailingStopActive;
        this.closeReason = closeReason;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.entryFee = entryFee;
        this.exitFee = exitFee;
        this.totalFees = totalFees;
    }

    public static Position open(String market, BigDecimal entryPrice, BigDecimal entryVolume,
                                 BigDecimal stopLossPrice, BigDecimal takeProfitPrice) {
        return open(market, entryPrice, entryVolume, stopLossPrice, takeProfitPrice, BigDecimal.ZERO);
    }

    public static Position open(String market, BigDecimal entryPrice, BigDecimal entryVolume,
                                 BigDecimal stopLossPrice, BigDecimal takeProfitPrice, BigDecimal entryFee) {
        BigDecimal entryAmount = entryPrice.multiply(entryVolume);
        return new Position(null, market, PositionStatus.OPEN,
                entryPrice, entryVolume, entryAmount,
                null, null, null, null, null,
                stopLossPrice, takeProfitPrice, null, entryPrice, false, null,
                LocalDateTime.now(), null, LocalDateTime.now(),
                entryFee != null ? entryFee : BigDecimal.ZERO, null, null);
    }

    public void close(BigDecimal exitPrice, BigDecimal exitVolume, CloseReason reason) {
        close(exitPrice, exitVolume, reason, BigDecimal.ZERO);
    }

    public void close(BigDecimal exitPrice, BigDecimal exitVolume, CloseReason reason, BigDecimal exitFee) {
        this.exitPrice = exitPrice;
        this.exitVolume = exitVolume;
        this.exitAmount = exitPrice.multiply(exitVolume);
        this.exitFee = exitFee != null ? exitFee : BigDecimal.ZERO;
        this.totalFees = (this.entryFee != null ? this.entryFee : BigDecimal.ZERO)
                .add(this.exitFee);
        // P&L 계산 시 수수료 차감
        this.realizedPnl = exitAmount.subtract(entryAmount).subtract(totalFees);
        this.realizedPnlPct = realizedPnl.divide(entryAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        this.closeReason = reason;
        this.status = PositionStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
        BigDecimal currentValue = currentPrice.multiply(entryVolume);
        return currentValue.subtract(entryAmount);
    }

    public BigDecimal calculateUnrealizedPnlPct(BigDecimal currentPrice) {
        BigDecimal unrealizedPnl = calculateUnrealizedPnl(currentPrice);
        return unrealizedPnl.divide(entryAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 수수료를 포함한 미실현 손익 계산
     * 진입 수수료 + 예상 청산 수수료를 차감
     *
     * @param currentPrice 현재가
     * @param feeRate 수수료율 (예: 0.0025 = 0.25%)
     * @return 수수료 차감 후 미실현 손익
     */
    public BigDecimal calculateUnrealizedPnlWithFee(BigDecimal currentPrice, BigDecimal feeRate) {
        BigDecimal currentValue = currentPrice.multiply(entryVolume);
        BigDecimal estimatedExitFee = currentValue.multiply(feeRate).setScale(0, RoundingMode.UP);
        BigDecimal totalFees = (entryFee != null ? entryFee : BigDecimal.ZERO).add(estimatedExitFee);
        return currentValue.subtract(entryAmount).subtract(totalFees);
    }

    /**
     * 수수료를 포함한 미실현 손익률 계산
     *
     * @param currentPrice 현재가
     * @param feeRate 수수료율 (예: 0.0025 = 0.25%)
     * @return 수수료 차감 후 미실현 손익률 (%)
     */
    public BigDecimal calculateUnrealizedPnlPctWithFee(BigDecimal currentPrice, BigDecimal feeRate) {
        BigDecimal unrealizedPnl = calculateUnrealizedPnlWithFee(currentPrice, feeRate);
        return unrealizedPnl.divide(entryAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean shouldStopLoss(BigDecimal currentPrice) {
        return stopLossPrice != null && currentPrice.compareTo(stopLossPrice) <= 0;
    }

    public boolean shouldTakeProfit(BigDecimal currentPrice) {
        return takeProfitPrice != null && currentPrice.compareTo(takeProfitPrice) >= 0;
    }

    public boolean isOpen() {
        return status == PositionStatus.OPEN;
    }

    public void updateHighWaterMark(BigDecimal currentPrice) {
        if (this.highWaterMark == null || currentPrice.compareTo(this.highWaterMark) > 0) {
            this.highWaterMark = currentPrice;
        }
    }

    public void activateTrailingStop(BigDecimal trailingStopPrice) {
        this.trailingStopActive = true;
        this.trailingStopPrice = trailingStopPrice;
    }

    public void updateTrailingStop(BigDecimal newTrailingStopPrice) {
        if (this.trailingStopPrice == null || newTrailingStopPrice.compareTo(this.trailingStopPrice) > 0) {
            this.trailingStopPrice = newTrailingStopPrice;
        }
    }

    public boolean shouldTrailingStop(BigDecimal currentPrice) {
        return trailingStopActive && trailingStopPrice != null &&
               currentPrice.compareTo(trailingStopPrice) <= 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public PositionStatus getStatus() { return status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public BigDecimal getEntryVolume() { return entryVolume; }
    public BigDecimal getEntryAmount() { return entryAmount; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public BigDecimal getExitVolume() { return exitVolume; }
    public BigDecimal getExitAmount() { return exitAmount; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getRealizedPnlPct() { return realizedPnlPct; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public BigDecimal getTakeProfitPrice() { return takeProfitPrice; }
    public void setTakeProfitPrice(BigDecimal takeProfitPrice) { this.takeProfitPrice = takeProfitPrice; }
    public BigDecimal getTrailingStopPrice() { return trailingStopPrice; }
    public void setTrailingStopPrice(BigDecimal trailingStopPrice) { this.trailingStopPrice = trailingStopPrice; }
    public BigDecimal getHighWaterMark() { return highWaterMark; }
    public void setHighWaterMark(BigDecimal highWaterMark) { this.highWaterMark = highWaterMark; }
    public boolean isTrailingStopActive() { return trailingStopActive; }
    public void setTrailingStopActive(boolean trailingStopActive) { this.trailingStopActive = trailingStopActive; }
    public CloseReason getCloseReason() { return closeReason; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getEntryFee() { return entryFee; }
    public BigDecimal getExitFee() { return exitFee; }
    public BigDecimal getTotalFees() { return totalFees; }
}
