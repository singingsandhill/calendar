package me.singingsandhill.calendar.stock.domain.candle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 캔들 (OHLCV) 엔티티
 */
public class StockCandle {

    private Long id;
    private final String stockCode;
    private final LocalDateTime candleDateTime;
    private final CandleInterval interval;
    private final BigDecimal openPrice;
    private final BigDecimal highPrice;
    private final BigDecimal lowPrice;
    private final BigDecimal closePrice;
    private final Long volume;
    private final BigDecimal tradeValue;
    private final LocalDateTime createdAt;

    private StockCandle(String stockCode, LocalDateTime candleDateTime, CandleInterval interval,
                        BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
                        BigDecimal closePrice, Long volume, BigDecimal tradeValue) {
        this.stockCode = stockCode;
        this.candleDateTime = candleDateTime;
        this.interval = interval;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradeValue = tradeValue;
        this.createdAt = LocalDateTime.now();
    }

    public static StockCandle of(String stockCode, LocalDateTime dateTime, CandleInterval interval,
                                  BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                                  Long volume, BigDecimal tradeValue) {
        return new StockCandle(stockCode, dateTime, interval, open, high, low, close, volume, tradeValue);
    }

    /**
     * 양봉 여부
     */
    public boolean isBullish() {
        return closePrice.compareTo(openPrice) > 0;
    }

    /**
     * 음봉 여부
     */
    public boolean isBearish() {
        return closePrice.compareTo(openPrice) < 0;
    }

    /**
     * 캔들 몸통 크기
     */
    public BigDecimal getBodySize() {
        return closePrice.subtract(openPrice).abs();
    }

    /**
     * 캔들 전체 범위
     */
    public BigDecimal getRange() {
        return highPrice.subtract(lowPrice);
    }

    // ========== Getters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public LocalDateTime getCandleDateTime() { return candleDateTime; }
    public CandleInterval getInterval() { return interval; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public Long getVolume() { return volume; }
    public BigDecimal getTradeValue() { return tradeValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
