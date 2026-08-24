package me.singingsandhill.calendar.stock.domain.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 포지션 엔티티
 * - 다단계 익절 지원 (TP1, TP2, TP3)
 * - 트레일링 스탑 관리
 */
public class StockPosition {

    private Long id;
    private Long stockId;
    private final String stockCode;
    private final LocalDate tradingDate;
    private StockPositionStatus status;

    // 진입 정보
    private BigDecimal entryPrice;
    private Integer entryQuantity;
    private BigDecimal entryAmount;
    private LocalDateTime enteredAt;

    // 현재 포지션
    private Integer remainingQuantity;
    private BigDecimal averageExitPrice;

    // 다단계 익절 추적
    private boolean tp1Executed;
    private boolean tp2Executed;
    private boolean tp3Executed;
    private BigDecimal dayHighPrice;

    // 손절/트레일링
    private BigDecimal stopLossPrice;
    private BigDecimal trailingHigh;
    private BigDecimal trailingStopPrice;
    private boolean trailingActive;

    // 손익
    private BigDecimal realizedPnl;
    private BigDecimal realizedPnlPercent;
    private BigDecimal totalExitAmount;
    private Integer totalExitQuantity;

    private StockCloseReason closeReason;
    private LocalDateTime closedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StockPosition(String stockCode, LocalDate tradingDate) {
        this.stockCode = stockCode;
        this.tradingDate = tradingDate;
        this.status = StockPositionStatus.OPEN;
        this.tp1Executed = false;
        this.tp2Executed = false;
        this.tp3Executed = false;
        this.trailingActive = false;
        this.realizedPnl = BigDecimal.ZERO;
        this.totalExitAmount = BigDecimal.ZERO;
        this.totalExitQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 새 포지션 오픈
     */
    public static StockPosition open(String stockCode, LocalDate tradingDate,
                                      BigDecimal entryPrice, Integer quantity,
                                      BigDecimal stopLossPrice, BigDecimal dayHighPrice) {
        StockPosition position = new StockPosition(stockCode, tradingDate);
        position.entryPrice = entryPrice;
        position.entryQuantity = quantity;
        position.entryAmount = entryPrice.multiply(BigDecimal.valueOf(quantity));
        position.remainingQuantity = quantity;
        position.stopLossPrice = stopLossPrice;
        position.dayHighPrice = dayHighPrice;
        position.enteredAt = LocalDateTime.now();
        return position;
    }

    /**
     * 손절가 산정 — 진입 근거(풀백저가 반등)가 깨지는 지점, 단 진입가 대비 최대 손실률로 캡.
     *
     * {@code SL = max(풀백저가 × (1 - buffer%), 진입가 × (1 - maxLoss%))}
     *
     * 고정 -5% 는 현실적 익절 폭(전고점 회복 시 +0.8~5% 의 부분청산) 대비 R:R 이 역전돼
     * 손익분기 승률 ~83% 를 요구했다 (2026-07-24 리뷰 §4). 풀백저가는 진입 논리의 무효화
     * 지점이므로 손절 기준으로 타당하고, 체결가가 풀백저가에서 크게 밀린 경우에는 캡이
     * 손실을 제한한다. 풀백저가가 없거나 비정상(진입가 이상)이면 캡만 적용.
     */
    public static BigDecimal resolveStopLossPrice(BigDecimal entryPrice, BigDecimal pullbackLow,
                                                   BigDecimal pullbackBufferPercent,
                                                   BigDecimal maxLossPercent) {
        BigDecimal cap = entryPrice.multiply(BigDecimal.ONE.subtract(
            maxLossPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));

        if (pullbackLow == null || pullbackLow.compareTo(BigDecimal.ZERO) <= 0) {
            return cap;
        }

        BigDecimal anchored = pullbackLow.multiply(BigDecimal.ONE.subtract(
            pullbackBufferPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));

        // 손절가는 진입가 아래여야 한다 — 풀백저가가 진입가 이상인 이상 데이터는 캡으로 폴백.
        if (anchored.compareTo(entryPrice) >= 0) {
            return cap;
        }
        return anchored.max(cap);
    }

    /**
     * 저장소에서 불러온 엔티티를 도메인 객체로 복원. 인프라 레이어 전용.
     */
    public static StockPosition reconstitute(
            Long id, Long stockId, String stockCode, LocalDate tradingDate,
            StockPositionStatus status,
            BigDecimal entryPrice, Integer entryQuantity, BigDecimal entryAmount,
            LocalDateTime enteredAt, Integer remainingQuantity, BigDecimal averageExitPrice,
            boolean tp1Executed, boolean tp2Executed, boolean tp3Executed,
            BigDecimal dayHighPrice, BigDecimal stopLossPrice,
            BigDecimal trailingHigh, BigDecimal trailingStopPrice, boolean trailingActive,
            BigDecimal realizedPnl, BigDecimal realizedPnlPercent,
            StockCloseReason closeReason, LocalDateTime closedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        StockPosition p = new StockPosition(stockCode, tradingDate);
        p.id = id;
        p.stockId = stockId;
        p.status = status != null ? status : StockPositionStatus.OPEN;
        p.entryPrice = entryPrice;
        p.entryQuantity = entryQuantity;
        p.entryAmount = entryAmount;
        p.enteredAt = enteredAt;
        p.remainingQuantity = remainingQuantity;
        p.averageExitPrice = averageExitPrice;
        p.tp1Executed = tp1Executed;
        p.tp2Executed = tp2Executed;
        p.tp3Executed = tp3Executed;
        p.dayHighPrice = dayHighPrice;
        p.stopLossPrice = stopLossPrice;
        p.trailingHigh = trailingHigh;
        p.trailingStopPrice = trailingStopPrice;
        p.trailingActive = trailingActive;
        p.realizedPnl = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        p.realizedPnlPercent = realizedPnlPercent;
        p.closeReason = closeReason;
        p.closedAt = closedAt;
        p.updatedAt = updatedAt != null ? updatedAt : p.createdAt;
        return p;
    }

    // ========== 손익 계산 ==========

    /**
     * 미실현 손익 계산
     */
    public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
        if (remainingQuantity == null || remainingQuantity == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        return currentValue.subtract(costBasis);
    }

    /**
     * 미실현 손익률 계산
     */
    public BigDecimal calculateUnrealizedPnlPercent(BigDecimal currentPrice) {
        if (entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(entryPrice)
            .multiply(new BigDecimal("100"))
            .divide(entryPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 수수료 포함 미실현 손익 계산
     * @param commissionRate 증권사 수수료율 (매수/매도 각각)
     * @param sellTaxRate 매도 세금율 (거래세+농특세)
     */
    public BigDecimal calculateUnrealizedPnlWithFee(BigDecimal currentPrice,
            BigDecimal commissionRate, BigDecimal sellTaxRate) {
        if (remainingQuantity == null || remainingQuantity == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        BigDecimal buyFee = costBasis.multiply(commissionRate);
        BigDecimal sellFee = currentValue.multiply(commissionRate.add(sellTaxRate));
        return currentValue.subtract(costBasis).subtract(buyFee).subtract(sellFee);
    }

    /**
     * 수수료 포함 미실현 손익률 계산 (%)
     * @param commissionRate 증권사 수수료율 (매수/매도 각각)
     * @param sellTaxRate 매도 세금율 (거래세+농특세)
     */
    public BigDecimal calculateUnrealizedPnlPctWithFee(BigDecimal currentPrice,
            BigDecimal commissionRate, BigDecimal sellTaxRate) {
        if (remainingQuantity == null || remainingQuantity == 0
                || entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal pnl = calculateUnrealizedPnlWithFee(currentPrice, commissionRate, sellTaxRate);
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(remainingQuantity));
        return pnl.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    // ========== 손절/익절 체크 ==========

    /**
     * 손절 조건 체크 (현재가가 resolveStopLossPrice 로 산정된 손절가 이하)
     */
    public boolean shouldStopLoss(BigDecimal currentPrice) {
        if (stopLossPrice == null) {
            return false;
        }
        return currentPrice.compareTo(stopLossPrice) <= 0;
    }

    /**
     * 1차 익절 조건 체크 (진입가 + tp1Percent — exit.tp1-percent)
     */
    public boolean shouldTp1(BigDecimal currentPrice, BigDecimal tp1Percent) {
        if (tp1Executed) {
            return false;
        }
        BigDecimal targetPrice = entryPrice.multiply(
            BigDecimal.ONE.add(tp1Percent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );
        return currentPrice.compareTo(targetPrice) >= 0;
    }

    /**
     * 2차 익절 조건 체크 (당일 고점). PR-4: TP1 선행 의존을 제거 — 독립 트리거.
     */
    public boolean shouldTp2(BigDecimal currentPrice) {
        if (tp2Executed || dayHighPrice == null) {
            return false;
        }
        return currentPrice.compareTo(dayHighPrice) >= 0;
    }

    /**
     * 3차 익절 조건 체크 (진입가 +tp3Percent). PR-4: TP2 선행 의존을 제거 — 독립 트리거.
     *
     * 앵커는 *진입가 고정* — 매 틱 갱신되는 당일고가를 앵커로 쓰면 5초 폴링 간격에 +N% 점프를
     * 요구해 수학적으로 도달 불가능했다 (2026-07-24 리뷰 §4 / P1-3).
     */
    public boolean shouldTp3(BigDecimal currentPrice, BigDecimal tp3Percent) {
        if (tp3Executed || entryPrice == null) {
            return false;
        }
        BigDecimal targetPrice = entryPrice.multiply(
            BigDecimal.ONE.add(tp3Percent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );
        return currentPrice.compareTo(targetPrice) >= 0;
    }

    /**
     * 트레일링 스탑 조건 체크
     */
    public boolean shouldTrailingStop(BigDecimal currentPrice) {
        if (!trailingActive || trailingStopPrice == null) {
            return false;
        }
        return currentPrice.compareTo(trailingStopPrice) <= 0;
    }

    // ========== 익절 수량 계산 ==========

    /**
     * 1차 익절 수량 (진입수량 × tp1Ratio, 잔여수량 캡).
     *
     * TP2(전고점 회복)가 TP1 보다 먼저 발동하는 것이 통상 경로라 잔여가 이미 줄어 있을 수
     * 있다 — 캡 없이는 초과 매도 주문이 나간다 (2026-07-24 리뷰 §3-⑤).
     */
    public int calculateTp1Quantity(BigDecimal tp1Ratio) {
        int target = BigDecimal.valueOf(entryQuantity).multiply(tp1Ratio)
            .setScale(0, RoundingMode.FLOOR).intValue();
        return Math.min(target, remainingQuantity);
    }

    /**
     * 2차 익절 수량 (잔여수량 × tp2Ratio)
     */
    public int calculateTp2Quantity(BigDecimal tp2Ratio) {
        return BigDecimal.valueOf(remainingQuantity).multiply(tp2Ratio)
            .setScale(0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 3차 익절 수량 (잔여 전량)
     */
    public int calculateTp3Quantity() {
        return remainingQuantity;
    }

    // ========== 포지션 업데이트 ==========

    /**
     * 부분 청산 실행 (수수료 포함)
     * @param commissionRate 증권사 수수료율 (매수/매도 각각)
     * @param sellTaxRate 매도 세금율 (거래세+농특세)
     */
    public void executePartialExit(int quantity, BigDecimal exitPrice, StockCloseReason reason,
                                    BigDecimal commissionRate, BigDecimal sellTaxRate) {
        if (quantity > remainingQuantity) {
            throw new IllegalArgumentException("Exit quantity exceeds remaining quantity");
        }

        BigDecimal exitAmount = exitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal costBasis = entryPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal buyFee = costBasis.multiply(commissionRate);
        BigDecimal sellFee = exitAmount.multiply(commissionRate.add(sellTaxRate));
        BigDecimal pnl = exitAmount.subtract(costBasis).subtract(buyFee).subtract(sellFee);

        this.totalExitAmount = this.totalExitAmount.add(exitAmount);
        this.totalExitQuantity = this.totalExitQuantity + quantity;
        this.remainingQuantity = this.remainingQuantity - quantity;
        this.realizedPnl = this.realizedPnl.add(pnl);

        // 평균 청산가 계산
        this.averageExitPrice = this.totalExitAmount.divide(
            BigDecimal.valueOf(this.totalExitQuantity), 2, RoundingMode.HALF_UP);

        // 익절 플래그 업데이트
        switch (reason) {
            case TP1 -> this.tp1Executed = true;
            case TP2 -> this.tp2Executed = true;
            case TP3 -> this.tp3Executed = true;
            default -> {}
        }

        // 상태 업데이트
        if (this.remainingQuantity == 0) {
            this.status = StockPositionStatus.CLOSED;
            this.closeReason = reason;
            this.closedAt = LocalDateTime.now();
        } else {
            this.status = StockPositionStatus.PARTIAL;
        }

        calculateRealizedPnlPercent();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔여 전량 청산 (수수료 포함)
     */
    public void closeRemaining(BigDecimal exitPrice, StockCloseReason reason,
                                BigDecimal commissionRate, BigDecimal sellTaxRate) {
        if (remainingQuantity == null || remainingQuantity == 0) {
            return;
        }
        executePartialExit(remainingQuantity, exitPrice, reason, commissionRate, sellTaxRate);
    }

    /**
     * 실현손익률 계산
     */
    private void calculateRealizedPnlPercent() {
        BigDecimal totalCost = entryPrice.multiply(BigDecimal.valueOf(totalExitQuantity));
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            this.realizedPnlPercent = BigDecimal.ZERO;
        } else {
            this.realizedPnlPercent = realizedPnl
                .multiply(new BigDecimal("100"))
                .divide(totalCost, 4, RoundingMode.HALF_UP);
        }
    }

    /**
     * 트레일링 스탑 업데이트 (손익분기점 보장)
     * @param breakEvenPrice 손익분기점 가격 (진입가 × (1 + 왕복수수료)), null이면 보장 안함
     */
    public void updateTrailingStop(BigDecimal currentPrice, BigDecimal trailingPercent,
                                    BigDecimal breakEvenPrice) {
        // 부분익절이 한 번이라도 발생하면 트레일링 활성화.
        // tp1Executed 만 조건으로 두면, TP2(전고점 회복)가 먼저 발동하는 통상 경로에서
        // 트레일링이 영원히 켜지지 않는다 (2026-07-24 리뷰 §4 / P1-2).
        if (!trailingActive && (tp1Executed || tp2Executed || tp3Executed)) {
            this.trailingActive = true;
            this.trailingHigh = currentPrice;
            // 활성화 시점에 스탑가를 즉시 세팅한다. 신고가 갱신 블록에서만 세팅하면
            // 활성화 직후 하락만 하는 경우 스탑가가 null 인 채 영원히 미발동한다.
            this.trailingStopPrice = calculateTrailingStop(currentPrice, trailingPercent, breakEvenPrice);
        }

        if (!trailingActive) {
            return;
        }

        // 고점 갱신
        if (currentPrice.compareTo(trailingHigh) > 0) {
            this.trailingHigh = currentPrice;
            this.trailingStopPrice = calculateTrailingStop(trailingHigh, trailingPercent, breakEvenPrice);
        }

        this.updatedAt = LocalDateTime.now();
    }

    /** 고점 대비 -trailingPercent, 단 손익분기점 아래로는 내려가지 않는다. */
    private BigDecimal calculateTrailingStop(BigDecimal high, BigDecimal trailingPercent,
                                              BigDecimal breakEvenPrice) {
        BigDecimal stop = high.multiply(
            BigDecimal.ONE.subtract(trailingPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );
        if (breakEvenPrice != null && stop.compareTo(breakEvenPrice) < 0) {
            return breakEvenPrice;
        }
        return stop;
    }

    /**
     * 당일 고점 업데이트
     */
    public void updateDayHighPrice(BigDecimal highPrice) {
        if (this.dayHighPrice == null || highPrice.compareTo(this.dayHighPrice) > 0) {
            this.dayHighPrice = highPrice;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // ========== Status Checks ==========

    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isClosed() {
        return status == StockPositionStatus.CLOSED;
    }

    public boolean hasRemainingQuantity() {
        return remainingQuantity != null && remainingQuantity > 0;
    }

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockCode() { return stockCode; }
    public LocalDate getTradingDate() { return tradingDate; }
    public StockPositionStatus getStatus() { return status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public Integer getEntryQuantity() { return entryQuantity; }
    public BigDecimal getEntryAmount() { return entryAmount; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public BigDecimal getAverageExitPrice() { return averageExitPrice; }
    public boolean isTp1Executed() { return tp1Executed; }
    public boolean isTp2Executed() { return tp2Executed; }
    public boolean isTp3Executed() { return tp3Executed; }
    public BigDecimal getDayHighPrice() { return dayHighPrice; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public BigDecimal getTrailingHigh() { return trailingHigh; }
    public BigDecimal getTrailingStopPrice() { return trailingStopPrice; }
    public boolean isTrailingActive() { return trailingActive; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getRealizedPnlPercent() { return realizedPnlPercent; }
    public StockCloseReason getCloseReason() { return closeReason; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
