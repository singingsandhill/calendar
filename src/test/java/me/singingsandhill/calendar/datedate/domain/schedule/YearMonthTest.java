package me.singingsandhill.calendar.datedate.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import me.singingsandhill.calendar.datedate.domain.schedule.YearMonth;

class YearMonthTest {

    @Test
    @DisplayName("Valid YearMonth should be created successfully")
    void validYearMonth_createsSuccessfully() {
        YearMonth ym = new YearMonth(2025, 12);

        assertThat(ym.year()).isEqualTo(2025);
        assertThat(ym.month()).isEqualTo(12);
    }

    @Test
    @DisplayName("Year before 2024 should throw exception")
    void yearBefore2024_throwsException() {
        assertThatThrownBy(() -> new YearMonth(2023, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Year");
    }

    @Test
    @DisplayName("Year after 2100 should throw exception")
    void yearAfter2100_throwsException() {
        assertThatThrownBy(() -> new YearMonth(2101, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Year");
    }

    @Test
    @DisplayName("Month less than 1 should throw exception")
    void monthLessThan1_throwsException() {
        assertThatThrownBy(() -> new YearMonth(2025, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month");
    }

    @Test
    @DisplayName("Month greater than 12 should throw exception")
    void monthGreaterThan12_throwsException() {
        assertThatThrownBy(() -> new YearMonth(2025, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month");
    }

    @ParameterizedTest
    @CsvSource({
            "2025, 1, 31",
            "2025, 2, 28",
            "2024, 2, 29",
            "2025, 4, 30",
            "2025, 12, 31"
    })
    @DisplayName("getDaysInMonth should return correct number of days")
    void getDaysInMonth_returnsCorrectValue(int year, int month, int expectedDays) {
        YearMonth ym = new YearMonth(year, month);

        assertThat(ym.getDaysInMonth()).isEqualTo(expectedDays);
    }

    @Test
    @DisplayName("calculateWeeks should return value between 4 and 6")
    void calculateWeeks_returnsBetween4And6() {
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = new YearMonth(2025, month);
            int weeks = ym.calculateWeeks();

            assertThat(weeks).isBetween(4, 6);
        }
    }

    @Test
    @DisplayName("getFirstDayOfWeek should return 0 for Sunday")
    void getFirstDayOfWeek_sundayIsZero() {
        YearMonth ym = new YearMonth(2025, 6);
        int firstDay = ym.getFirstDayOfWeek();

        assertThat(firstDay).isBetween(0, 6);
    }

    @Test
    @DisplayName("dateOf — 7주 확장 모드는 그리드 시작(1일이 속한 주의 일요일)부터 센다")
    void dateOf_extendedMode() {
        YearMonth oct = new YearMonth(2026, 10);   // 10-01 목요일 → 그리드 시작 09-27(일)
        assertThat(oct.dateOf(1, 7)).contains(java.time.LocalDate.of(2026, 9, 27));
        assertThat(oct.dateOf(5, 7)).contains(java.time.LocalDate.of(2026, 10, 1));
        assertThat(oct.dateOf(49, 7)).contains(java.time.LocalDate.of(2026, 11, 14));
        assertThat(oct.dateOf(0, 7)).isEmpty();
        assertThat(oct.dateOf(50, 7)).isEmpty();
    }

    @Test
    @DisplayName("dateOf — 기존 모드(weeks != 7)는 인덱스 = 그 달의 날짜 (확장 규칙이면 요일이 어긋난다)")
    void dateOf_legacyMode() {
        YearMonth oct = new YearMonth(2026, 10);
        assertThat(oct.dateOf(1, 5)).contains(java.time.LocalDate.of(2026, 10, 1));
        assertThat(oct.dateOf(31, 5)).contains(java.time.LocalDate.of(2026, 10, 31));
        assertThat(oct.dateOf(32, 5)).isEmpty();
        assertThat(oct.dateOf(0, 5)).isEmpty();
    }
}
