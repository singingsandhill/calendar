package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-2: 서킷브레이커 — 연속 손실 / 일일 손실 한도 도달 시 신규 진입 차단.
 */
class TradingCircuitBreakerTest {

    private TradingProperties props;
    private TradingCircuitBreaker breaker;

    private static final BigDecimal EQUITY = new BigDecimal("1000000"); // 시작 자본 100만
    private static final BigDecimal SMALL_LOSS = new BigDecimal("-10000"); // -1%

    @BeforeEach
    void setUp() {
        props = new TradingProperties();
        props.getRisk().setCircuitBreakerEnabled(true);
        props.getRisk().setMaxConsecutiveLosses(3);
        props.getRisk().setMaxDailyLossPct(-0.05);
        breaker = new TradingCircuitBreaker(props);
    }

    @Test
    void belowConsecutiveLossThreshold_doesNotBlock() {
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        assertThat(breaker.getConsecutiveLosses()).isEqualTo(2);
        assertThat(breaker.isEntryBlocked(EQUITY, SMALL_LOSS)).isFalse();
    }

    @Test
    void reachingConsecutiveLossThreshold_blocksEntry() {
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        assertThat(breaker.getConsecutiveLosses()).isEqualTo(3);
        assertThat(breaker.isEntryBlocked(EQUITY, SMALL_LOSS)).isTrue();
    }

    @Test
    void aWinResetsConsecutiveLossStreak() {
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("500")); // 승 → 리셋
        assertThat(breaker.getConsecutiveLosses()).isZero();
        assertThat(breaker.isEntryBlocked(EQUITY, SMALL_LOSS)).isFalse();
    }

    @Test
    void dailyLossBeyondLimit_blocksEntry() {
        // 일일 실현 -6% (≤ -5%) → 차단
        assertThat(breaker.isEntryBlocked(EQUITY, new BigDecimal("-60000"))).isTrue();
    }

    @Test
    void dailyLossWithinLimit_doesNotBlock() {
        // 일일 실현 -4% (> -5%) → 통과
        assertThat(breaker.isEntryBlocked(EQUITY, new BigDecimal("-40000"))).isFalse();
    }

    @Test
    void disabled_neverBlocks() {
        props.getRisk().setCircuitBreakerEnabled(false);
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        breaker.recordOutcome(new BigDecimal("-100"));
        assertThat(breaker.isEntryBlocked(EQUITY, new BigDecimal("-100000"))).isFalse();
    }
}
