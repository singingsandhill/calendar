package me.singingsandhill.calendar.stock.domain.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 감시 종목 엔티티
 * - 갭 상승 스크리닝을 통과한 종목
 * - 눌림목 패턴 감지를 위한 상태 관리
 */
public class Stock {

    private Long id;
    private final String stockCode;
    private final String stockName;
    private final LocalDate tradingDate;

    // 전일 데이터
    private BigDecimal prevClosePrice;
    private Long prevVolume;

    // 당일 데이터
    private BigDecimal openPrice;
    private BigDecimal currentPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private BigDecimal tradeValue;

    // 스크리닝 지표
    private BigDecimal gapPercent;
    private BigDecimal marketCap;
    private BigDecimal tradeStrength;
    private BigDecimal spreadPercent;

    // 상태 관리
    private StockState state;
    private BigDecimal highAfterOpen;
    private LocalDateTime highFormedAt;
    private BigDecimal pullbackLow;
    private LocalDateTime pullbackStartAt;
    private BigDecimal entryPrice;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Stock(String stockCode, String stockName, LocalDate tradingDate) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("Stock code cannot be null or blank");
        }
        if (stockName == null || stockName.isBlank()) {
            throw new IllegalArgumentException("Stock name cannot be null or blank");
        }
        if (tradingDate == null) {
            throw new IllegalArgumentException("Trading date cannot be null");
        }

        this.stockCode = stockCode;
        this.stockName = stockName;
        this.tradingDate = tradingDate;
        this.state = StockState.WATCHING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // ========== Domain Methods ==========

    /**
     * 갭 비율 계산
     */
    public BigDecimal calculateGapPercent() {
        if (prevClosePrice == null || prevClosePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return openPrice.subtract(prevClosePrice)
            .multiply(new BigDecimal("100"))
            .divide(prevClosePrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 갭 필터 통과 여부 (2.0% ~ 7.0%)
     */
    public boolean passesGapFilter(BigDecimal minGap, BigDecimal maxGap) {
        if (gapPercent == null) {
            return false;
        }
        return gapPercent.compareTo(minGap) >= 0 && gapPercent.compareTo(maxGap) <= 0;
    }

    /**
     * 시가총액 필터 통과 여부
     */
    public boolean passesMarketCapFilter(BigDecimal minMarketCap) {
        if (marketCap == null) {
            return false;
        }
        return marketCap.compareTo(minMarketCap) >= 0;
    }

    /**
     * 체결강도 필터 통과 여부
     */
    public boolean passesTradeStrengthFilter(BigDecimal minStrength) {
        if (tradeStrength == null) {
            return false;
        }
        return tradeStrength.compareTo(minStrength) >= 0;
    }

    /**
     * 스프레드 필터 통과 여부
     */
    public boolean passesSpreadFilter(BigDecimal maxSpread) {
        if (spreadPercent == null) {
            return true;
        }
        return spreadPercent.compareTo(maxSpread) <= 0;
    }

    /**
     * 시가 대비 상승률 계산
     */
    public BigDecimal calculateReturnFromOpen() {
        if (openPrice == null || openPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(openPrice)
            .multiply(new BigDecimal("100"))
            .divide(openPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 고점 대비 하락률 계산
     */
    public BigDecimal calculateDropFromHigh() {
        if (highAfterOpen == null || highAfterOpen.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(highAfterOpen)
            .multiply(new BigDecimal("100"))
            .divide(highAfterOpen, 4, RoundingMode.HALF_UP);
    }

    /**
     * 눌림목 저점 대비 반등률 계산
     */
    public BigDecimal calculateBounceFromLow() {
        if (pullbackLow == null || pullbackLow.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(pullbackLow)
            .multiply(new BigDecimal("100"))
            .divide(pullbackLow, 4, RoundingMode.HALF_UP);
    }

    /**
     * 고점 형성 조건 충족 여부 (시가 +1.5% 이상)
     */
    public boolean isHighFormed(BigDecimal threshold) {
        BigDecimal returnFromOpen = calculateReturnFromOpen();
        return returnFromOpen.compareTo(threshold) >= 0;
    }

    /**
     * 눌림목 범위 내 여부 (고점 대비 -1.5% ~ -3.0%)
     */
    public boolean isInPullbackRange(BigDecimal minPullback, BigDecimal maxPullback) {
        BigDecimal dropFromHigh = calculateDropFromHigh();
        return dropFromHigh.compareTo(minPullback.negate()) <= 0
            && dropFromHigh.compareTo(maxPullback.negate()) >= 0;
    }

    /**
     * 눌림목 과도 여부 (고점 대비 -3.0% 초과 하락)
     */
    public boolean isPullbackTooDeep(BigDecimal maxPullback) {
        BigDecimal dropFromHigh = calculateDropFromHigh();
        return dropFromHigh.compareTo(maxPullback.negate()) < 0;
    }

    /**
     * 반등 확인 (저점 대비 +0.3%)
     */
    public boolean isBounceConfirmed(BigDecimal threshold) {
        BigDecimal bounceFromLow = calculateBounceFromLow();
        return bounceFromLow.compareTo(threshold) >= 0;
    }

    // ========== State Transitions ==========

    /**
     * 상태 업데이트
     */
    public void updateState(StockState newState) {
        this.state = newState;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 고점 형성 기록
     */
    public void recordHighFormed(BigDecimal highPrice) {
        this.highAfterOpen = highPrice;
        this.highFormedAt = LocalDateTime.now();
        this.state = StockState.HIGH_FORMED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 눌림목 시작 기록
     */
    public void recordPullbackStart(BigDecimal lowPrice) {
        this.pullbackLow = lowPrice;
        this.pullbackStartAt = LocalDateTime.now();
        this.state = StockState.PULLBACK;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 눌림목 저점 갱신
     */
    public void updatePullbackLow(BigDecimal lowPrice) {
        if (this.pullbackLow == null || lowPrice.compareTo(this.pullbackLow) < 0) {
            this.pullbackLow = lowPrice;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 진입 준비 상태로 전환
     */
    public void markEntryReady() {
        this.state = StockState.ENTRY_READY;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 진입 완료 기록
     */
    public void markEntered(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
        this.state = StockState.ENTERED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 청산 완료 기록
     */
    public void markExited() {
        this.state = StockState.EXITED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 필터 아웃 처리
     */
    public void markFilteredOut() {
        this.state = StockState.FILTERED_OUT;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Price Updates ==========

    /**
     * 현재가 업데이트
     */
    public void updateCurrentPrice(BigDecimal price) {
        this.currentPrice = price;

        // 고가 갱신
        if (this.highPrice == null || price.compareTo(this.highPrice) > 0) {
            this.highPrice = price;
        }

        // 저가 갱신
        if (this.lowPrice == null || price.compareTo(this.lowPrice) < 0) {
            this.lowPrice = price;
        }

        this.updatedAt = LocalDateTime.now();
    }

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public LocalDate getTradingDate() { return tradingDate; }
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
    public StockState getState() { return state; }
    public BigDecimal getHighAfterOpen() { return highAfterOpen; }
    public LocalDateTime getHighFormedAt() { return highFormedAt; }
    public BigDecimal getPullbackLow() { return pullbackLow; }
    public LocalDateTime getPullbackStartAt() { return pullbackStartAt; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
