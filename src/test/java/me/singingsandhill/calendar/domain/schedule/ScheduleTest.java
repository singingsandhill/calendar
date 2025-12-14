package me.singingsandhill.calendar.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    @DisplayName("Schedule creation with valid parameters should succeed")
    void validSchedule_createsSuccessfully() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.getOwnerId()).isEqualTo("test-user");
        assertThat(schedule.getYear()).isEqualTo(2025);
        assertThat(schedule.getMonth()).isEqualTo(12);
        assertThat(schedule.getWeeks()).isGreaterThanOrEqualTo(4);
        assertThat(schedule.getWeeks()).isLessThanOrEqualTo(6);
        assertThat(schedule.getParticipants()).isEmpty();
    }

    @Test
    @DisplayName("Schedule with explicit weeks should use provided value")
    void explicitWeeks_usesProvidedValue() {
        Schedule schedule = new Schedule("test-user", 2025, 12, 5);

        assertThat(schedule.getWeeks()).isEqualTo(5);
    }

    @Test
    @DisplayName("Schedule should auto-calculate weeks when not provided")
    void nullWeeks_autoCalculates() {
        Schedule schedule = new Schedule("test-user", 2025, 12, null);

        assertThat(schedule.getWeeks()).isNotNull();
        assertThat(schedule.getWeeks()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("getDaysInMonth should return correct value for December")
    void daysInMonth_december() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.getDaysInMonth()).isEqualTo(31);
    }

    @Test
    @DisplayName("getDaysInMonth should return correct value for February in leap year")
    void daysInMonth_februaryLeapYear() {
        Schedule schedule = new Schedule("test-user", 2024, 2);

        assertThat(schedule.getDaysInMonth()).isEqualTo(29);
    }

    @Test
    @DisplayName("getDaysInMonth should return correct value for February in non-leap year")
    void daysInMonth_februaryNonLeapYear() {
        Schedule schedule = new Schedule("test-user", 2025, 2);

        assertThat(schedule.getDaysInMonth()).isEqualTo(28);
    }

    @Test
    @DisplayName("canAddParticipant should return true when under limit")
    void canAddParticipant_underLimit() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.canAddParticipant()).isTrue();
    }

    @Test
    @DisplayName("Invalid year should throw exception")
    void invalidYear_throwsException() {
        assertThatThrownBy(() -> new Schedule("test-user", 2020, 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Invalid month should throw exception")
    void invalidMonth_throwsException() {
        assertThatThrownBy(() -> new Schedule("test-user", 2025, 13))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
