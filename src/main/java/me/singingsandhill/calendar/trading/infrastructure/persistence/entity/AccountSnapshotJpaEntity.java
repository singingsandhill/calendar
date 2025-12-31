package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_account_snapshots")
public class AccountSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "krw_balance", nullable = false, precision = 20, scale = 8)
    private BigDecimal krwBalance;

    @Column(name = "ada_balance", nullable = false, precision = 20, scale = 8)
    private BigDecimal adaBalance;

    @Column(name = "ada_avg_price", precision = 20, scale = 8)
    private BigDecimal adaAvgPrice;

    @Column(name = "current_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "total_value_krw", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalValueKrw;

    @Column(name = "ada_ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal adaRatio;

    @Column(name = "unrealized_pnl", precision = 20, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "unrealized_pnl_pct", precision = 10, scale = 4)
    private BigDecimal unrealizedPnlPct;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AccountSnapshotJpaEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    public BigDecimal getKrwBalance() { return krwBalance; }
    public void setKrwBalance(BigDecimal krwBalance) { this.krwBalance = krwBalance; }
    public BigDecimal getAdaBalance() { return adaBalance; }
    public void setAdaBalance(BigDecimal adaBalance) { this.adaBalance = adaBalance; }
    public BigDecimal getAdaAvgPrice() { return adaAvgPrice; }
    public void setAdaAvgPrice(BigDecimal adaAvgPrice) { this.adaAvgPrice = adaAvgPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getTotalValueKrw() { return totalValueKrw; }
    public void setTotalValueKrw(BigDecimal totalValueKrw) { this.totalValueKrw = totalValueKrw; }
    public BigDecimal getAdaRatio() { return adaRatio; }
    public void setAdaRatio(BigDecimal adaRatio) { this.adaRatio = adaRatio; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }
    public BigDecimal getUnrealizedPnlPct() { return unrealizedPnlPct; }
    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) { this.unrealizedPnlPct = unrealizedPnlPct; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
