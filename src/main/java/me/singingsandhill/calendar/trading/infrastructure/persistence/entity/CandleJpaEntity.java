package me.singingsandhill.calendar.trading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_candles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"market", "candle_date_time"}))
public class CandleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(name = "candle_date_time", nullable = false)
    private LocalDateTime candleDateTime;

    @Column(name = "opening_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal openingPrice;

    @Column(name = "high_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal lowPrice;

    @Column(name = "trade_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal tradePrice;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal volume;

    @Column(name = "acc_trade_price", precision = 20, scale = 8)
    private BigDecimal accTradePrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CandleJpaEntity() {}

    public CandleJpaEntity(String market, LocalDateTime candleDateTime,
                           BigDecimal openingPrice, BigDecimal highPrice, BigDecimal lowPrice,
                           BigDecimal tradePrice, BigDecimal volume, BigDecimal accTradePrice,
                           LocalDateTime createdAt) {
        this.market = market;
        this.candleDateTime = candleDateTime;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.volume = volume;
        this.accTradePrice = accTradePrice;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public LocalDateTime getCandleDateTime() { return candleDateTime; }
    public void setCandleDateTime(LocalDateTime candleDateTime) { this.candleDateTime = candleDateTime; }
    public BigDecimal getOpeningPrice() { return openingPrice; }
    public void setOpeningPrice(BigDecimal openingPrice) { this.openingPrice = openingPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
    public BigDecimal getTradePrice() { return tradePrice; }
    public void setTradePrice(BigDecimal tradePrice) { this.tradePrice = tradePrice; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public BigDecimal getAccTradePrice() { return accTradePrice; }
    public void setAccTradePrice(BigDecimal accTradePrice) { this.accTradePrice = accTradePrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
