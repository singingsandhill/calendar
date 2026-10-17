package me.singingsandhill.calendar.datedate.domain.participant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantAvailabilityTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Test
    @DisplayName("고른 인덱스를 실제 날짜로 — 0·범위 밖·null 은 건너뛰고 중복은 한 번만")
    void convertsAndSkipsInvalid() {
        ParticipantAvailability a = new ParticipantAvailability(1L, 2026, 10, 5, CREATED,
                Arrays.asList(1, 0, 32, null, 3, 3));
        assertThat(a.availableDates()).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    }

    @Test
    @DisplayName("selections 가 null·빈 목록이면 빈 결과")
    void emptySelections() {
        assertThat(new ParticipantAvailability(1L, 2026, 10, 7, CREATED, null).availableDates()).isEmpty();
        assertThat(new ParticipantAvailability(1L, 2026, 10, 7, CREATED, List.of()).availableDates()).isEmpty();
    }
}
