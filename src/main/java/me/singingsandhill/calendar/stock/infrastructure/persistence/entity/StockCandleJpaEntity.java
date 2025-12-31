package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_candles",
       indexes = {
           @Index(name = "idx_candle_stock_interval", columnList = "stock_code, interval_type, candle_datetime DESC")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_candle", columnNames = {"stock_code", "candle_datetime", "interval_type"})
       })
public class StockCandleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "candle_datetime", nullable = false)
    private LocalDateTime candleDatetime;

    @Column(name = "interval_type", nullable = false, length = 10)
    private String intervalType;

    @Column(name = "open_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal closePrice;

    @Column(nullable = false)
    private Long volume;

    @Column(name = "trade_value", precision = 20, scale = 0)
    private BigDecimal tradeValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StockCandleJpaEntity() {}

    public StockCandleJpaEntity(String stockCode, LocalDateTime candleDatetime, String intervalType,
                                 BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
                                 BigDecimal closePrice, Long volume, BigDecimal tradeValue) {
        this.stockCode = stockCode;
        this.candleDatetime = candleDatetime;
        this.intervalType = intervalType;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradeValue = tradeValue;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public LocalDateTime getCandleDatetime() { return candleDatetime; }
    public void setCandleDatetime(LocalDateTime candleDatetime) { this.candleDatetime = candleDatetime; }
    public String getIntervalType() { return intervalType; }
    public void setIntervalType(String intervalType) { this.intervalType = intervalType; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public BigDecimal getTradeValue() { return tradeValue; }
    public void setTradeValue(BigDecimal tradeValue) { this.tradeValue = tradeValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
