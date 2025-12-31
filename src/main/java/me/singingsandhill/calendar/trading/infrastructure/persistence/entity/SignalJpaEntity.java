package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_signals")
public class SignalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(name = "signal_time", nullable = false)
    private LocalDateTime signalTime;

    @Column(name = "signal_type", nullable = false, length = 10)
    private String signalType;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "ma_cross_score")
    private Integer maCrossScore;

    @Column(name = "ma_trend_score")
    private Integer maTrendScore;

    @Column(name = "rsi_divergence_score")
    private Integer rsiDivergenceScore;

    @Column(name = "rsi_level_score")
    private Integer rsiLevelScore;

    @Column(name = "stoch_divergence_score")
    private Integer stochDivergenceScore;

    @Column(name = "stoch_level_score")
    private Integer stochLevelScore;

    @Column(name = "volume_divergence_score")
    private Integer volumeDivergenceScore;

    @Column(precision = 20, scale = 8)
    private BigDecimal ma5;

    @Column(precision = 20, scale = 8)
    private BigDecimal ma20;

    @Column(precision = 20, scale = 8)
    private BigDecimal ma60;

    @Column(precision = 10, scale = 4)
    private BigDecimal rsi;

    @Column(name = "stoch_k", precision = 10, scale = 4)
    private BigDecimal stochK;

    @Column(name = "stoch_d", precision = 10, scale = 4)
    private BigDecimal stochD;

    @Column(name = "rsi_divergence", length = 10)
    private String rsiDivergence;

    @Column(name = "stoch_divergence", length = 10)
    private String stochDivergence;

    @Column(name = "volume_divergence", length = 10)
    private String volumeDivergence;

    @Column(name = "current_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private boolean executed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SignalJpaEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public LocalDateTime getSignalTime() { return signalTime; }
    public void setSignalTime(LocalDateTime signalTime) { this.signalTime = signalTime; }
    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public Integer getMaCrossScore() { return maCrossScore; }
    public void setMaCrossScore(Integer maCrossScore) { this.maCrossScore = maCrossScore; }
    public Integer getMaTrendScore() { return maTrendScore; }
    public void setMaTrendScore(Integer maTrendScore) { this.maTrendScore = maTrendScore; }
    public Integer getRsiDivergenceScore() { return rsiDivergenceScore; }
    public void setRsiDivergenceScore(Integer rsiDivergenceScore) { this.rsiDivergenceScore = rsiDivergenceScore; }
    public Integer getRsiLevelScore() { return rsiLevelScore; }
    public void setRsiLevelScore(Integer rsiLevelScore) { this.rsiLevelScore = rsiLevelScore; }
    public Integer getStochDivergenceScore() { return stochDivergenceScore; }
    public void setStochDivergenceScore(Integer stochDivergenceScore) { this.stochDivergenceScore = stochDivergenceScore; }
    public Integer getStochLevelScore() { return stochLevelScore; }
    public void setStochLevelScore(Integer stochLevelScore) { this.stochLevelScore = stochLevelScore; }
    public Integer getVolumeDivergenceScore() { return volumeDivergenceScore; }
    public void setVolumeDivergenceScore(Integer volumeDivergenceScore) { this.volumeDivergenceScore = volumeDivergenceScore; }
    public BigDecimal getMa5() { return ma5; }
    public void setMa5(BigDecimal ma5) { this.ma5 = ma5; }
    public BigDecimal getMa20() { return ma20; }
    public void setMa20(BigDecimal ma20) { this.ma20 = ma20; }
    public BigDecimal getMa60() { return ma60; }
    public void setMa60(BigDecimal ma60) { this.ma60 = ma60; }
    public BigDecimal getRsi() { return rsi; }
    public void setRsi(BigDecimal rsi) { this.rsi = rsi; }
    public BigDecimal getStochK() { return stochK; }
    public void setStochK(BigDecimal stochK) { this.stochK = stochK; }
    public BigDecimal getStochD() { return stochD; }
    public void setStochD(BigDecimal stochD) { this.stochD = stochD; }
    public String getRsiDivergence() { return rsiDivergence; }
    public void setRsiDivergence(String rsiDivergence) { this.rsiDivergence = rsiDivergence; }
    public String getStochDivergence() { return stochDivergence; }
    public void setStochDivergence(String stochDivergence) { this.stochDivergence = stochDivergence; }
    public String getVolumeDivergence() { return volumeDivergence; }
    public void setVolumeDivergence(String volumeDivergence) { this.volumeDivergence = volumeDivergence; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
