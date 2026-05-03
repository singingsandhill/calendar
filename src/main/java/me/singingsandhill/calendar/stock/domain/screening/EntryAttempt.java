package me.singingsandhill.calendar.stock.domain.screening;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 진입 시도 기록.
 * 통과/실패 모든 케이스를 적재해 후속 분석(승률, 거부 사유 분포)에 활용한다.
 */
public class EntryAttempt {

    private Long id;
    private LocalDate tradingDate;
    private String stockCode;
    private LocalDateTime attemptedAt;
    private boolean accepted;
    private int passedConditions;
    private int requiredConditions;
    private boolean strengthPassed;
    private boolean imbalancePassed;
    private boolean timePassed;
    private BigDecimal currentPrice;
    private BigDecimal pullbackLow;
    private String rejectReason;

    protected EntryAttempt() {}

    public static EntryAttempt of(LocalDate tradingDate, String stockCode,
                                   boolean accepted, int passed, int required,
                                   boolean strengthPassed, boolean imbalancePassed, boolean timePassed,
                                   BigDecimal currentPrice, BigDecimal pullbackLow, String rejectReason) {
        EntryAttempt a = new EntryAttempt();
        a.tradingDate = tradingDate;
        a.stockCode = stockCode;
        a.attemptedAt = LocalDateTime.now();
        a.accepted = accepted;
        a.passedConditions = passed;
        a.requiredConditions = required;
        a.strengthPassed = strengthPassed;
        a.imbalancePassed = imbalancePassed;
        a.timePassed = timePassed;
        a.currentPrice = currentPrice;
        a.pullbackLow = pullbackLow;
        a.rejectReason = rejectReason;
        return a;
    }

    public static EntryAttempt restore(Long id, LocalDate tradingDate, String stockCode,
                                        LocalDateTime attemptedAt, boolean accepted,
                                        int passedConditions, int requiredConditions,
                                        boolean strengthPassed, boolean imbalancePassed, boolean timePassed,
                                        BigDecimal currentPrice, BigDecimal pullbackLow, String rejectReason) {
        EntryAttempt a = new EntryAttempt();
        a.id = id;
        a.tradingDate = tradingDate;
        a.stockCode = stockCode;
        a.attemptedAt = attemptedAt;
        a.accepted = accepted;
        a.passedConditions = passedConditions;
        a.requiredConditions = requiredConditions;
        a.strengthPassed = strengthPassed;
        a.imbalancePassed = imbalancePassed;
        a.timePassed = timePassed;
        a.currentPrice = currentPrice;
        a.pullbackLow = pullbackLow;
        a.rejectReason = rejectReason;
        return a;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getTradingDate() { return tradingDate; }
    public String getStockCode() { return stockCode; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public boolean isAccepted() { return accepted; }
    public int getPassedConditions() { return passedConditions; }
    public int getRequiredConditions() { return requiredConditions; }
    public boolean isStrengthPassed() { return strengthPassed; }
    public boolean isImbalancePassed() { return imbalancePassed; }
    public boolean isTimePassed() { return timePassed; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getPullbackLow() { return pullbackLow; }
    public String getRejectReason() { return rejectReason; }
}
