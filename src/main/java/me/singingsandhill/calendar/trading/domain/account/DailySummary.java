package me.singingsandhill.calendar.trading.domain.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailySummary {

    private Long id;
    private final LocalDate summaryDate;
    private int tradeCount;
    private int buyCount;
    private int sellCount;
    private BigDecimal realizedPnl;
    private BigDecimal totalVolume;
    private BigDecimal totalAmount;
    private int winCount;
    private int loseCount;
    private BigDecimal winRate;
    private BigDecimal startBalance;
    private BigDecimal endBalance;
    private final LocalDateTime createdAt;

    public DailySummary(Long id, LocalDate summaryDate, int tradeCount, int buyCount, int sellCount,
                        BigDecimal realizedPnl, BigDecimal totalVolume, BigDecimal totalAmount,
                        int winCount, int loseCount, BigDecimal winRate,
                        BigDecimal startBalance, BigDecimal endBalance, LocalDateTime createdAt) {
        this.id = id;
        this.summaryDate = summaryDate;
        this.tradeCount = tradeCount;
        this.buyCount = buyCount;
        this.sellCount = sellCount;
        this.realizedPnl = realizedPnl;
        this.totalVolume = totalVolume;
        this.totalAmount = totalAmount;
        this.winCount = winCount;
        this.loseCount = loseCount;
        this.winRate = winRate;
        this.startBalance = startBalance;
        this.endBalance = endBalance;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static DailySummary createEmpty(LocalDate date, BigDecimal startBalance) {
        return new DailySummary(null, date, 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, BigDecimal.ZERO, startBalance, startBalance, LocalDateTime.now());
    }

    public void addTrade(boolean isBuy, BigDecimal volume, BigDecimal amount) {
        this.tradeCount++;
        if (isBuy) {
            this.buyCount++;
        } else {
            this.sellCount++;
        }
        this.totalVolume = this.totalVolume.add(volume);
        this.totalAmount = this.totalAmount.add(amount);
    }

    public void addClosedPosition(BigDecimal pnl) {
        this.realizedPnl = this.realizedPnl.add(pnl);
        if (pnl.compareTo(BigDecimal.ZERO) > 0) {
            this.winCount++;
        } else if (pnl.compareTo(BigDecimal.ZERO) < 0) {
            this.loseCount++;
        }
        updateWinRate();
    }

    public void setEndBalance(BigDecimal endBalance) {
        this.endBalance = endBalance;
    }

    private void updateWinRate() {
        int totalClosed = winCount + loseCount;
        if (totalClosed > 0) {
            this.winRate = BigDecimal.valueOf(winCount)
                    .divide(BigDecimal.valueOf(totalClosed), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public int getTradeCount() { return tradeCount; }
    public int getBuyCount() { return buyCount; }
    public int getSellCount() { return sellCount; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getWinCount() { return winCount; }
    public int getLoseCount() { return loseCount; }
    public BigDecimal getWinRate() { return winRate; }
    public BigDecimal getStartBalance() { return startBalance; }
    public BigDecimal getEndBalance() { return endBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
