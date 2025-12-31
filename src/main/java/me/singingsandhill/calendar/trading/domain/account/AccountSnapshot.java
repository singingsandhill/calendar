package me.singingsandhill.calendar.trading.domain.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class AccountSnapshot {

    private Long id;
    private final LocalDateTime snapshotTime;
    private final BigDecimal krwBalance;
    private final BigDecimal adaBalance;
    private final BigDecimal adaAvgPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal totalValueKrw;
    private final BigDecimal adaRatio;
    private final BigDecimal unrealizedPnl;
    private final BigDecimal unrealizedPnlPct;
    private final LocalDateTime createdAt;

    public AccountSnapshot(Long id, LocalDateTime snapshotTime,
                           BigDecimal krwBalance, BigDecimal adaBalance, BigDecimal adaAvgPrice,
                           BigDecimal currentPrice, BigDecimal totalValueKrw, BigDecimal adaRatio,
                           BigDecimal unrealizedPnl, BigDecimal unrealizedPnlPct,
                           LocalDateTime createdAt) {
        this.id = id;
        this.snapshotTime = snapshotTime;
        this.krwBalance = krwBalance;
        this.adaBalance = adaBalance;
        this.adaAvgPrice = adaAvgPrice;
        this.currentPrice = currentPrice;
        this.totalValueKrw = totalValueKrw;
        this.adaRatio = adaRatio;
        this.unrealizedPnl = unrealizedPnl;
        this.unrealizedPnlPct = unrealizedPnlPct;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static AccountSnapshot create(BigDecimal krwBalance, BigDecimal adaBalance,
                                          BigDecimal adaAvgPrice, BigDecimal currentPrice) {
        BigDecimal adaValue = adaBalance.multiply(currentPrice);
        BigDecimal totalValue = krwBalance.add(adaValue);
        BigDecimal adaRatio = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? adaValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        BigDecimal unrealizedPnlPct = BigDecimal.ZERO;
        if (adaAvgPrice != null && adaAvgPrice.compareTo(BigDecimal.ZERO) > 0
                && adaBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costBasis = adaBalance.multiply(adaAvgPrice);
            unrealizedPnl = adaValue.subtract(costBasis);
            unrealizedPnlPct = unrealizedPnl.divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new AccountSnapshot(null, LocalDateTime.now(),
                krwBalance, adaBalance, adaAvgPrice, currentPrice,
                totalValue, adaRatio, unrealizedPnl, unrealizedPnlPct, LocalDateTime.now());
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public BigDecimal getKrwBalance() { return krwBalance; }
    public BigDecimal getAdaBalance() { return adaBalance; }
    public BigDecimal getAdaAvgPrice() { return adaAvgPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getTotalValueKrw() { return totalValueKrw; }
    public BigDecimal getAdaRatio() { return adaRatio; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public BigDecimal getUnrealizedPnlPct() { return unrealizedPnlPct; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
