package me.singingsandhill.calendar.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_entry_attempts",
       indexes = {
           @Index(name = "idx_eattempt_date", columnList = "tradingDate"),
           @Index(name = "idx_eattempt_code", columnList = "stockCode")
       })
public class EntryAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate tradingDate;

    @Column(length = 16)
    private String stockCode;

    private LocalDateTime attemptedAt;

    private boolean accepted;
    private int passedConditions;
    private int requiredConditions;
    private boolean strengthPassed;
    private boolean imbalancePassed;
    private boolean timePassed;

    @Column(precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal pullbackLow;

    @Column(length = 64)
    private String rejectReason;

    protected EntryAttemptJpaEntity() {}

    public static EntryAttemptJpaEntity fromDomain(EntryAttempt a) {
        EntryAttemptJpaEntity e = new EntryAttemptJpaEntity();
        e.id = a.getId();
        e.tradingDate = a.getTradingDate();
        e.stockCode = a.getStockCode();
        e.attemptedAt = a.getAttemptedAt();
        e.accepted = a.isAccepted();
        e.passedConditions = a.getPassedConditions();
        e.requiredConditions = a.getRequiredConditions();
        e.strengthPassed = a.isStrengthPassed();
        e.imbalancePassed = a.isImbalancePassed();
        e.timePassed = a.isTimePassed();
        e.currentPrice = a.getCurrentPrice();
        e.pullbackLow = a.getPullbackLow();
        e.rejectReason = a.getRejectReason();
        return e;
    }

    public EntryAttempt toDomain() {
        return EntryAttempt.restore(id, tradingDate, stockCode, attemptedAt, accepted,
            passedConditions, requiredConditions,
            strengthPassed, imbalancePassed, timePassed,
            currentPrice, pullbackLow, rejectReason);
    }

    public Long getId() { return id; }
}
