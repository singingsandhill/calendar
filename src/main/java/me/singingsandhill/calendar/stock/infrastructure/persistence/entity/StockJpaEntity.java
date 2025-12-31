package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_monitoring",
       indexes = {
           @Index(name = "idx_stock_date_state", columnList = "trading_date, state"),
           @Index(name = "idx_stock_code_date", columnList = "stock_code, trading_date")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_stock_code_date", columnNames = {"stock_code", "trading_date"})
       })
public class StockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "trading_date", nullable = false)
    private LocalDate tradingDate;

    @Column(name = "prev_close_price", precision = 15, scale = 2)
    private BigDecimal prevClosePrice;

    @Column(name = "prev_volume")
    private Long prevVolume;

    @Column(name = "open_price", precision = 15, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 15, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "trade_value", precision = 20, scale = 0)
    private BigDecimal tradeValue;

    @Column(name = "gap_percent", precision = 10, scale = 4)
    private BigDecimal gapPercent;

    @Column(name = "market_cap", precision = 20, scale = 0)
    private BigDecimal marketCap;

    @Column(name = "trade_strength", precision = 10, scale = 2)
    private BigDecimal tradeStrength;

    @Column(name = "spread_percent", precision = 10, scale = 4)
    private BigDecimal spreadPercent;

    @Column(nullable = false, length = 20)
    private String state;

    @Column(name = "high_after_open", precision = 15, scale = 2)
    private BigDecimal highAfterOpen;

    @Column(name = "high_formed_at")
    private LocalDateTime highFormedAt;

    @Column(name = "pullback_low", precision = 15, scale = 2)
    private BigDecimal pullbackLow;

    @Column(name = "pullback_start_at")
    private LocalDateTime pullbackStartAt;

    @Column(name = "entry_price", precision = 15, scale = 2)
    private BigDecimal entryPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected StockJpaEntity() {}

    public StockJpaEntity(String stockCode, String stockName, LocalDate tradingDate, String state) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.tradingDate = tradingDate;
        this.state = state;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public LocalDate getTradingDate() { return tradingDate; }
    public void setTradingDate(LocalDate tradingDate) { this.tradingDate = tradingDate; }
    public BigDecimal getPrevClosePrice() { return prevClosePrice; }
    public void setPrevClosePrice(BigDecimal prevClosePrice) { this.prevClosePrice = prevClosePrice; }
    public Long getPrevVolume() { return prevVolume; }
    public void setPrevVolume(Long prevVolume) { this.prevVolume = prevVolume; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public BigDecimal getTradeValue() { return tradeValue; }
    public void setTradeValue(BigDecimal tradeValue) { this.tradeValue = tradeValue; }
    public BigDecimal getGapPercent() { return gapPercent; }
    public void setGapPercent(BigDecimal gapPercent) { this.gapPercent = gapPercent; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getTradeStrength() { return tradeStrength; }
    public void setTradeStrength(BigDecimal tradeStrength) { this.tradeStrength = tradeStrength; }
    public BigDecimal getSpreadPercent() { return spreadPercent; }
    public void setSpreadPercent(BigDecimal spreadPercent) { this.spreadPercent = spreadPercent; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getHighAfterOpen() { return highAfterOpen; }
    public void setHighAfterOpen(BigDecimal highAfterOpen) { this.highAfterOpen = highAfterOpen; }
    public LocalDateTime getHighFormedAt() { return highFormedAt; }
    public void setHighFormedAt(LocalDateTime highFormedAt) { this.highFormedAt = highFormedAt; }
    public BigDecimal getPullbackLow() { return pullbackLow; }
    public void setPullbackLow(BigDecimal pullbackLow) { this.pullbackLow = pullbackLow; }
    public LocalDateTime getPullbackStartAt() { return pullbackStartAt; }
    public void setPullbackStartAt(LocalDateTime pullbackStartAt) { this.pullbackStartAt = pullbackStartAt; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
