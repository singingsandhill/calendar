package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_signals",
       indexes = {
           @Index(name = "idx_signal_stock_time", columnList = "stock_code, signal_time DESC")
       })
public class StockSignalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "signal_time", nullable = false)
    private LocalDateTime signalTime;

    @Column(name = "signal_type", nullable = false, length = 30)
    private String signalType;

    @Column(name = "gap_percent", precision = 10, scale = 4)
    private BigDecimal gapPercent;

    @Column(name = "market_cap", precision = 20, scale = 0)
    private BigDecimal marketCap;

    @Column(name = "trade_value", precision = 20, scale = 0)
    private BigDecimal tradeValue;

    @Column(name = "trade_strength", precision = 10, scale = 2)
    private BigDecimal tradeStrength;

    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "pullback_percent", precision = 10, scale = 4)
    private BigDecimal pullbackPercent;

    @Column(name = "bounce_percent", precision = 10, scale = 4)
    private BigDecimal bouncePercent;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column
    private Boolean executed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StockSignalJpaEntity() {}

    public StockSignalJpaEntity(String stockCode, LocalDateTime signalTime, String signalType) {
        this.stockCode = stockCode;
        this.signalTime = signalTime;
        this.signalType = signalType;
        this.executed = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public LocalDateTime getSignalTime() { return signalTime; }
    public void setSignalTime(LocalDateTime signalTime) { this.signalTime = signalTime; }
    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }
    public BigDecimal getGapPercent() { return gapPercent; }
    public void setGapPercent(BigDecimal gapPercent) { this.gapPercent = gapPercent; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getTradeValue() { return tradeValue; }
    public void setTradeValue(BigDecimal tradeValue) { this.tradeValue = tradeValue; }
    public BigDecimal getTradeStrength() { return tradeStrength; }
    public void setTradeStrength(BigDecimal tradeStrength) { this.tradeStrength = tradeStrength; }
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    public BigDecimal getPullbackPercent() { return pullbackPercent; }
    public void setPullbackPercent(BigDecimal pullbackPercent) { this.pullbackPercent = pullbackPercent; }
    public BigDecimal getBouncePercent() { return bouncePercent; }
    public void setBouncePercent(BigDecimal bouncePercent) { this.bouncePercent = bouncePercent; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public Boolean getExecuted() { return executed; }
    public void setExecuted(Boolean executed) { this.executed = executed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
