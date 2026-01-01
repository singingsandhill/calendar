package me.singingsandhill.calendar.trading.domain.signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Signal {

    private Long id;
    private final String market;
    private final LocalDateTime signalTime;
    private final SignalType signalType;
    private final int totalScore;
    private final Integer maCrossScore;
    private final Integer maTrendScore;
    private final Integer rsiDivergenceScore;
    private final Integer rsiLevelScore;
    private final Integer stochDivergenceScore;
    private final Integer stochLevelScore;
    private final Integer volumeDivergenceScore;
    private final Integer rsiTrendScore;
    private final BigDecimal ma5;
    private final BigDecimal ma20;
    private final BigDecimal ma60;
    private final BigDecimal rsi;
    private final BigDecimal stochK;
    private final BigDecimal stochD;
    private final DivergenceType rsiDivergence;
    private final DivergenceType stochDivergence;
    private final DivergenceType volumeDivergence;
    private final BigDecimal currentPrice;
    private boolean executed;
    private final LocalDateTime createdAt;

    public Signal(Long id, String market, LocalDateTime signalTime, SignalType signalType,
                  int totalScore, Integer maCrossScore, Integer maTrendScore,
                  Integer rsiDivergenceScore, Integer rsiLevelScore,
                  Integer stochDivergenceScore, Integer stochLevelScore,
                  Integer volumeDivergenceScore, Integer rsiTrendScore,
                  BigDecimal ma5, BigDecimal ma20, BigDecimal ma60,
                  BigDecimal rsi, BigDecimal stochK, BigDecimal stochD,
                  DivergenceType rsiDivergence, DivergenceType stochDivergence, DivergenceType volumeDivergence,
                  BigDecimal currentPrice, boolean executed, LocalDateTime createdAt) {
        this.id = id;
        this.market = market;
        this.signalTime = signalTime;
        this.signalType = signalType;
        this.totalScore = totalScore;
        this.maCrossScore = maCrossScore;
        this.maTrendScore = maTrendScore;
        this.rsiDivergenceScore = rsiDivergenceScore;
        this.rsiLevelScore = rsiLevelScore;
        this.stochDivergenceScore = stochDivergenceScore;
        this.stochLevelScore = stochLevelScore;
        this.volumeDivergenceScore = volumeDivergenceScore;
        this.rsiTrendScore = rsiTrendScore;
        this.ma5 = ma5;
        this.ma20 = ma20;
        this.ma60 = ma60;
        this.rsi = rsi;
        this.stochK = stochK;
        this.stochD = stochD;
        this.rsiDivergence = rsiDivergence;
        this.stochDivergence = stochDivergence;
        this.volumeDivergence = volumeDivergence;
        this.currentPrice = currentPrice;
        this.executed = executed;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public boolean hasDivergence() {
        return (rsiDivergence != null && rsiDivergence != DivergenceType.NONE) ||
               (stochDivergence != null && stochDivergence != DivergenceType.NONE) ||
               (volumeDivergence != null && volumeDivergence != DivergenceType.NONE);
    }

    public int countDivergences() {
        int count = 0;
        if (rsiDivergence != null && rsiDivergence != DivergenceType.NONE) count++;
        if (stochDivergence != null && stochDivergence != DivergenceType.NONE) count++;
        if (volumeDivergence != null && volumeDivergence != DivergenceType.NONE) count++;
        return count;
    }

    public boolean isBuySignal() {
        return signalType == SignalType.BUY;
    }

    public boolean isSellSignal() {
        return signalType == SignalType.SELL;
    }

    public void markExecuted() {
        this.executed = true;
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public LocalDateTime getSignalTime() { return signalTime; }
    public SignalType getSignalType() { return signalType; }
    public int getTotalScore() { return totalScore; }
    public Integer getMaCrossScore() { return maCrossScore; }
    public Integer getMaTrendScore() { return maTrendScore; }
    public Integer getRsiDivergenceScore() { return rsiDivergenceScore; }
    public Integer getRsiLevelScore() { return rsiLevelScore; }
    public Integer getStochDivergenceScore() { return stochDivergenceScore; }
    public Integer getStochLevelScore() { return stochLevelScore; }
    public Integer getVolumeDivergenceScore() { return volumeDivergenceScore; }
    public Integer getRsiTrendScore() { return rsiTrendScore; }
    public BigDecimal getMa5() { return ma5; }
    public BigDecimal getMa20() { return ma20; }
    public BigDecimal getMa60() { return ma60; }
    public BigDecimal getRsi() { return rsi; }
    public BigDecimal getStochK() { return stochK; }
    public BigDecimal getStochD() { return stochD; }
    public DivergenceType getRsiDivergence() { return rsiDivergence; }
    public DivergenceType getStochDivergence() { return stochDivergence; }
    public DivergenceType getVolumeDivergence() { return volumeDivergence; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public boolean isExecuted() { return executed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
