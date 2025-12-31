package me.singingsandhill.calendar.trading.domain.candle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Candle {

    private Long id;
    private final String market;
    private final LocalDateTime candleDateTime;
    private final BigDecimal openingPrice;
    private final BigDecimal highPrice;
    private final BigDecimal lowPrice;
    private final BigDecimal tradePrice;
    private final BigDecimal volume;
    private final BigDecimal accTradePrice;
    private final LocalDateTime createdAt;

    public Candle(Long id, String market, LocalDateTime candleDateTime,
                  BigDecimal openingPrice, BigDecimal highPrice, BigDecimal lowPrice,
                  BigDecimal tradePrice, BigDecimal volume, BigDecimal accTradePrice,
                  LocalDateTime createdAt) {
        validateNotNull(market, "market");
        validateNotNull(candleDateTime, "candleDateTime");
        validateNotNull(openingPrice, "openingPrice");
        validateNotNull(highPrice, "highPrice");
        validateNotNull(lowPrice, "lowPrice");
        validateNotNull(tradePrice, "tradePrice");
        validateNotNull(volume, "volume");

        this.id = id;
        this.market = market;
        this.candleDateTime = candleDateTime;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.volume = volume;
        this.accTradePrice = accTradePrice;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Candle of(String market, LocalDateTime candleDateTime,
                            BigDecimal openingPrice, BigDecimal highPrice, BigDecimal lowPrice,
                            BigDecimal tradePrice, BigDecimal volume, BigDecimal accTradePrice) {
        return new Candle(null, market, candleDateTime, openingPrice, highPrice, lowPrice,
                tradePrice, volume, accTradePrice, LocalDateTime.now());
    }

    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMarket() { return market; }
    public LocalDateTime getCandleDateTime() { return candleDateTime; }
    public BigDecimal getOpeningPrice() { return openingPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public BigDecimal getTradePrice() { return tradePrice; }
    public BigDecimal getVolume() { return volume; }
    public BigDecimal getAccTradePrice() { return accTradePrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
