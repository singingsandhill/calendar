package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_daily_summaries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"summary_date"}))
public class DailySummaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(name = "buy_count", nullable = false)
    private int buyCount;

    @Column(name = "sell_count", nullable = false)
    private int sellCount;

    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "total_volume", precision = 20, scale = 8)
    private BigDecimal totalVolume;

    @Column(name = "total_amount", precision = 20, scale = 8)
    private BigDecimal totalAmount;

    @Column(name = "win_count")
    private int winCount;

    @Column(name = "lose_count")
    private int loseCount;

    @Column(name = "win_rate", precision = 10, scale = 4)
    private BigDecimal winRate;

    @Column(name = "start_balance", precision = 20, scale = 8)
    private BigDecimal startBalance;

    @Column(name = "end_balance", precision = 20, scale = 8)
    private BigDecimal endBalance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DailySummaryJpaEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public int getTradeCount() { return tradeCount; }
    public void setTradeCount(int tradeCount) { this.tradeCount = tradeCount; }
    public int getBuyCount() { return buyCount; }
    public void setBuyCount(int buyCount) { this.buyCount = buyCount; }
    public int getSellCount() { return sellCount; }
    public void setSellCount(int sellCount) { this.sellCount = sellCount; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public int getWinCount() { return winCount; }
    public void setWinCount(int winCount) { this.winCount = winCount; }
    public int getLoseCount() { return loseCount; }
    public void setLoseCount(int loseCount) { this.loseCount = loseCount; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getStartBalance() { return startBalance; }
    public void setStartBalance(BigDecimal startBalance) { this.startBalance = startBalance; }
    public BigDecimal getEndBalance() { return endBalance; }
    public void setEndBalance(BigDecimal endBalance) { this.endBalance = endBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
