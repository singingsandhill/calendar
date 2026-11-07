package me.singingsandhill.calendar.trading.domain.position;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 보유 중 최대 유리·불리 폭 (ADR trading/observability/0004).
 *
 * HWM 만 있고 최저가가 없어 "손절된 포지션이 그 전에 얼마나 올랐었나 / 익절한 포지션이 얼마나 빠졌었나" 를
 * 포지션 데이터만으로 볼 수 없었다 — 출구 규칙 대안 평가의 기본 재료.
 */
class PositionExcursionTest {

    private Position openAt(String entry) {
        BigDecimal p = new BigDecimal(entry);
        return Position.open("KRW-ADA", p, BigDecimal.TEN, p.multiply(new BigDecimal("0.985")),
            p.multiply(new BigDecimal("1.03")));
    }

    @Test
    void lowWaterMarkStartsAtEntryAndFollowsNewLows() {
        Position position = openAt("290");

        assertThat(position.getLowWaterMark()).isEqualByComparingTo("290");
        position.updateLowWaterMark(new BigDecimal("288"));
        position.updateLowWaterMark(new BigDecimal("289"));   // 더 높으면 갱신하지 않는다

        assertThat(position.getLowWaterMark()).isEqualByComparingTo("288");
    }

    @Test
    void excursionsAreMeasuredFromEntryPrice() {
        Position position = openAt("290");
        position.updateHighWaterMark(new BigDecimal("295"));
        position.updateLowWaterMark(new BigDecimal("286"));

        // (295/290 − 1) × 100 = 1.7241, (286/290 − 1) × 100 = −1.3793
        assertThat(position.maxFavorableExcursionPct()).isEqualByComparingTo("1.7241");
        assertThat(position.maxAdverseExcursionPct()).isEqualByComparingTo("-1.3793");
    }

    @Test
    void legacyPositionWithoutLowWaterMarkHasNoAdverseExcursion() {
        Position legacy = new Position(1L, "KRW-ADA", PositionStatus.CLOSED,
            new BigDecimal("290"), BigDecimal.TEN, new BigDecimal("2900"),
            null, null, null, null, null, null, null, null, new BigDecimal("292"), false,
            null, null, null, null, null, null, null);

        assertThat(legacy.maxAdverseExcursionPct()).isNull();
        assertThat(legacy.maxFavorableExcursionPct()).isEqualByComparingTo("0.6897");
    }

    @Test
    void entrySourceAndModeAreCarriedOnTheDomainObject() {
        Position position = openAt("290");

        position.setEntrySource(EntrySource.REBALANCE);

        assertThat(position.getEntrySource()).isEqualTo(EntrySource.REBALANCE);
        assertThat(position.getMode()).isNull();   // insert 시 어댑터가 현재 모드를 찍는다
    }
}
