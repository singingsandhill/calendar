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
    private BigDecimal compositeScore;

    // 상태 관리
    private StockState state;
    private BigDecimal highAfterOpen;
    private LocalDateTime highFormedAt;
    private BigDecimal pullbackLow;
    private LocalDateTime pullbackStartAt;
    private BigDecimal entryPrice;

    // 진입(주문 단계) 실패 추적 — ADR stock/infrastructure/0009
    private int entryFailCount;
    private EntryOutcome lastEntryFailReason;
    private LocalDateTime lastEntryFailAt;
    private StockProductType productType;

    private LocalDateTime createdAt;
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
     * 갭 필터 통과 여부 (screening.min-gap-percent ~ max-gap-percent)
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
     * 눌림목 범위 내 여부 (고점 대비 -1.5% ~ -5.0%)
     */
    public boolean isInPullbackRange(BigDecimal minPullback, BigDecimal maxPullback) {
        BigDecimal dropFromHigh = calculateDropFromHigh();
        return dropFromHigh.compareTo(minPullback.negate()) <= 0
            && dropFromHigh.compareTo(maxPullback.negate()) >= 0;
    }

    /**
     * 눌림목 과도 여부 (고점 대비 -5.0% 초과 하락)
     */
    public boolean isPullbackTooDeep(BigDecimal maxPullback) {
        BigDecimal dropFromHigh = calculateDropFromHigh();
        return dropFromHigh.compareTo(maxPullback.negate()) < 0;
    }

    /**
     * 반등 확인 (저점 대비 +0.2%)
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
     * 영속화된 상태머신 작업 필드 복원 (인프라 어댑터 전용).
     *
     * 상태 전이 계산(calculateDropFromHigh/calculateBounceFromLow)은 이 필드들에 의존하는데
     * null 이면 예외 없이 0% 를 반환하므로, 재로딩 시 복원이 누락되면 HIGH_FORMED 에서
     * 조용히 영구 정체한다 (2026-07-24 리뷰 §3-①). record* 전이 메서드와 달리 상태·시각을
     * 새로 쓰지 않고 저장된 값을 그대로 되돌린다.
     */
    public void restorePersistedState(BigDecimal highAfterOpen, LocalDateTime highFormedAt,
                                       BigDecimal pullbackLow, LocalDateTime pullbackStartAt,
                                       BigDecimal entryPrice) {
        this.highAfterOpen = highAfterOpen;
        this.highFormedAt = highFormedAt;
        this.pullbackLow = pullbackLow;
        this.pullbackStartAt = pullbackStartAt;
        this.entryPrice = entryPrice;
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

    /**
     * 진입(주문 단계) 실패 기록 (ADR stock/infrastructure/0009).
     *
     * 사유·시각은 매번 남기고, 재시도 횟수는 {@link EntryOutcome#countsAsAttempt()} 인 실패만 센다 —
     * ORDER_UNCONFIRMED 는 접수 여부 판정을 미확인 주문 스윕이 하므로 횟수·상태를 건드리지 않는다.
     * 센 횟수가 maxAttempts 에 닿으면 ENTRY_READY → ENTRY_FAILED (그날 재시도 중단).
     *
     * @return 이번 기록으로 ENTRY_FAILED 로 전이했으면 true
     */
    public boolean recordEntryFailure(EntryOutcome reason, LocalDateTime at, int maxAttempts) {
        this.lastEntryFailReason = reason;
        this.lastEntryFailAt = at;
        this.updatedAt = LocalDateTime.now();
        if (!reason.countsAsAttempt()) {
            return false;
        }
        this.entryFailCount++;
        if (entryFailCount >= maxAttempts && state == StockState.ENTRY_READY) {
            this.state = StockState.ENTRY_FAILED;
            return true;
        }
        return false;
    }

    /** 직전 실패로부터 재시도 간격이 지났는지 — 실패 이력이 없으면 항상 true. */
    public boolean isEntryRetryDue(LocalDateTime now, int retryIntervalSeconds) {
        return lastEntryFailAt == null || !now.isBefore(lastEntryFailAt.plusSeconds(retryIntervalSeconds));
    }

    /**
     * 영속화된 생성·수정 시각 복원 (인프라 어댑터 전용) — 다른 복원·상태 갱신이 updatedAt 을 now 로 바꾸므로
     * 마지막에 부른다. 빠지면 감시 종목 createdAt 이 매 조회 시각이 된다 (ADR stock/observability/0003).
     */
    public void restoreAuditTimestamps(LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /** 영속화된 진입 실패 추적 필드 복원 (인프라 어댑터 전용). */
    public void restoreEntryFailure(int entryFailCount, EntryOutcome lastEntryFailReason,
                                    LocalDateTime lastEntryFailAt) {
        this.entryFailCount = entryFailCount;
        this.lastEntryFailReason = lastEntryFailReason;
        this.lastEntryFailAt = lastEntryFailAt;
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
    public BigDecimal getCompositeScore() { return compositeScore; }
    public void setCompositeScore(BigDecimal compositeScore) { this.compositeScore = compositeScore; }
    public StockState getState() { return state; }
    public BigDecimal getHighAfterOpen() { return highAfterOpen; }
    public LocalDateTime getHighFormedAt() { return highFormedAt; }
    public BigDecimal getPullbackLow() { return pullbackLow; }
    public LocalDateTime getPullbackStartAt() { return pullbackStartAt; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public int getEntryFailCount() { return entryFailCount; }
    public EntryOutcome getLastEntryFailReason() { return lastEntryFailReason; }
    public LocalDateTime getLastEntryFailAt() { return lastEntryFailAt; }
    public StockProductType getProductType() { return productType; }
    public void setProductType(StockProductType productType) { this.productType = productType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
