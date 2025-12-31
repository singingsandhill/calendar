package me.singingsandhill.calendar.stock.domain.signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 매매 신호 엔티티 (감사/분석용)
 */
public class StockSignal {

    private Long id;
    private final String stockCode;
    private final LocalDateTime signalTime;
    private final StockSignalType signalType;

    // 갭 감지 데이터
    private BigDecimal gapPercent;
    private BigDecimal marketCap;
    private BigDecimal tradeValue;
    private BigDecimal tradeStrength;

    // 눌림목 감지 데이터
    private BigDecimal highPrice;
    private BigDecimal pullbackPercent;
    private BigDecimal bouncePercent;
    private BigDecimal currentPrice;

    // 실행 추적
    private boolean executed;
    private final LocalDateTime createdAt;

    public StockSignal(String stockCode, StockSignalType signalType) {
        this.stockCode = stockCode;
        this.signalType = signalType;
        this.signalTime = LocalDateTime.now();
        this.executed = false;
        this.createdAt = LocalDateTime.now();
    }

    public static StockSignal gapDetected(String stockCode, BigDecimal gapPercent,
                                           BigDecimal marketCap, BigDecimal tradeValue,
                                           BigDecimal tradeStrength) {
        StockSignal signal = new StockSignal(stockCode, StockSignalType.GAP_DETECTED);
        signal.gapPercent = gapPercent;
        signal.marketCap = marketCap;
        signal.tradeValue = tradeValue;
        signal.tradeStrength = tradeStrength;
        return signal;
    }

    public static StockSignal highFormed(String stockCode, BigDecimal highPrice, BigDecimal currentPrice) {
        StockSignal signal = new StockSignal(stockCode, StockSignalType.HIGH_FORMED);
        signal.highPrice = highPrice;
        signal.currentPrice = currentPrice;
        return signal;
    }

    public static StockSignal pullbackEntry(String stockCode, BigDecimal highPrice,
                                             BigDecimal pullbackPercent, BigDecimal bouncePercent,
                                             BigDecimal currentPrice) {
        StockSignal signal = new StockSignal(stockCode, StockSignalType.PULLBACK_ENTRY);
        signal.highPrice = highPrice;
        signal.pullbackPercent = pullbackPercent;
        signal.bouncePercent = bouncePercent;
        signal.currentPrice = currentPrice;
        return signal;
    }

    public static StockSignal exitSignal(String stockCode, StockSignalType type, BigDecimal currentPrice) {
        StockSignal signal = new StockSignal(stockCode, type);
        signal.currentPrice = currentPrice;
        return signal;
    }

    public void markExecuted() {
        this.executed = true;
    }

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public LocalDateTime getSignalTime() { return signalTime; }
    public StockSignalType getSignalType() { return signalType; }
    public BigDecimal getGapPercent() { return gapPercent; }
    public BigDecimal getMarketCap() { return marketCap; }
    public BigDecimal getTradeValue() { return tradeValue; }
    public BigDecimal getTradeStrength() { return tradeStrength; }
    public BigDecimal getHighPrice() { return highPrice; }
    public BigDecimal getPullbackPercent() { return pullbackPercent; }
    public BigDecimal getBouncePercent() { return bouncePercent; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public boolean isExecuted() { return executed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
