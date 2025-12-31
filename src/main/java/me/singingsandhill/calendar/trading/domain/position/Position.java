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
    private CloseReason closeReason;
    private final LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private final LocalDateTime createdAt;

    public Position(Long id, String market, PositionStatus status,
                    BigDecimal entryPrice, BigDecimal entryVolume, BigDecimal entryAmount,
                    BigDecimal exitPrice, BigDecimal exitVolume, BigDecimal exitAmount,
                    BigDecimal realizedPnl, BigDecimal realizedPnlPct,
                    BigDecimal stopLossPrice, BigDecimal takeProfitPrice,
                    CloseReason closeReason, LocalDateTime openedAt, LocalDateTime closedAt,
                    LocalDateTime createdAt) {
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
        this.closeReason = closeReason;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Position open(String market, BigDecimal entryPrice, BigDecimal entryVolume,
                                 BigDecimal stopLossPrice, BigDecimal takeProfitPrice) {
        BigDecimal entryAmount = entryPrice.multiply(entryVolume);
        return new Position(null, market, PositionStatus.OPEN,
                entryPrice, entryVolume, entryAmount,
                null, null, null, null, null,
                stopLossPrice, takeProfitPrice, null,
                LocalDateTime.now(), null, LocalDateTime.now());
    }

    public void close(BigDecimal exitPrice, BigDecimal exitVolume, CloseReason reason) {
        this.exitPrice = exitPrice;
        this.exitVolume = exitVolume;
        this.exitAmount = exitPrice.multiply(exitVolume);
        this.realizedPnl = exitAmount.subtract(entryAmount);
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

    public boolean shouldStopLoss(BigDecimal currentPrice) {
        return stopLossPrice != null && currentPrice.compareTo(stopLossPrice) <= 0;
    }

    public boolean shouldTakeProfit(BigDecimal currentPrice) {
        return takeProfitPrice != null && currentPrice.compareTo(takeProfitPrice) >= 0;
    }

    public boolean isOpen() {
        return status == PositionStatus.OPEN;
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
    public CloseReason getCloseReason() { return closeReason; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
