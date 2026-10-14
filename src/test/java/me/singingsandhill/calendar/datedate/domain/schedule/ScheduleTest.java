package me.singingsandhill.calendar.datedate.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateParticipantException;
import me.singingsandhill.calendar.datedate.application.exception.ParticipantLimitExceededException;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;

class ScheduleTest {

    @Test
    @DisplayName("Schedule creation with valid parameters should succeed")
    void validSchedule_createsSuccessfully() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.getOwnerId()).isEqualTo("test-user");
        assertThat(schedule.getYear()).isEqualTo(2025);
        assertThat(schedule.getMonth()).isEqualTo(12);
        assertThat(schedule.getWeeks()).isEqualTo(7); // 새 일정은 7주 확장 모드
        assertThat(schedule.isExtendedMode()).isTrue();
        assertThat(schedule.getTotalDays()).isEqualTo(49);
        assertThat(schedule.getParticipants()).isEmpty();
    }

    @Test
    @DisplayName("Schedule with explicit weeks should use provided value")
    void explicitWeeks_usesProvidedValue() {
        Schedule schedule = new Schedule("test-user", 2025, 12, 5);

        assertThat(schedule.getWeeks()).isEqualTo(5);
    }

    @Test
    @DisplayName("Schedule should use 7 weeks when not provided")
    void nullWeeks_uses7Weeks() {
        Schedule schedule = new Schedule("test-user", 2025, 12, null);

        assertThat(schedule.getWeeks()).isEqualTo(7); // 기본값은 7주 확장 모드
        assertThat(schedule.isExtendedMode()).isTrue();
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

    @Test
    @DisplayName("addParticipant should accept up to MAX_PARTICIPANTS")
    void addParticipant_belowLimit_succeeds() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        for (int i = 0; i < Schedule.MAX_PARTICIPANTS; i++) {
            schedule.addParticipant(new Participant(1L, "user" + i, schedule.nextColorIndex()));
        }

        assertThat(schedule.getParticipantCount()).isEqualTo(Schedule.MAX_PARTICIPANTS);
        assertThat(schedule.canAddParticipant()).isFalse();
    }

    @Test
    @DisplayName("addParticipant beyond MAX_PARTICIPANTS should throw")
    void addParticipant_exceedsLimit_throws() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        for (int i = 0; i < Schedule.MAX_PARTICIPANTS; i++) {
            schedule.addParticipant(new Participant(1L, "user" + i, schedule.nextColorIndex()));
        }

        assertThatThrownBy(() -> schedule.addParticipant(new Participant(1L, "overflow", 0)))
                .isInstanceOf(ParticipantLimitExceededException.class);
    }

    @Test
    @DisplayName("참여자 한도는 10명이다 (ADR datedate/domain/0010)")
    void maxParticipants_isTen() {
        assertThat(Schedule.MAX_PARTICIPANTS).isEqualTo(10);
    }

    @Test
    @DisplayName("한도까지 채운 참여자는 모두 다른 색이다 — 한도를 올리면 팔레트도 늘려야 한다")
    void participantsUpToLimit_haveDistinctColors() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        for (int i = 0; i < Schedule.MAX_PARTICIPANTS; i++) {
            schedule.addParticipant(new Participant(1L, "user" + i, schedule.nextColorIndex()));
        }

        assertThat(schedule.getParticipants())
                .extracting(Participant::getColorHex)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("배포 전부터 8명이던 일정의 9·10번째 참여자는 새 2색을 받는다")
    void existingEightParticipantSchedule_ninthAndTenthGetNewColors() {
        List<Participant> eight = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            eight.add(new Participant((long) i + 1, 1L, "user" + i, ParticipantColor.ofIndex(i),
                    List.of(), LocalDateTime.now()));
        }
        Schedule schedule = new Schedule(1L, "test-user", 2025, 12, 7, LocalDateTime.now(), eight, null);

        Participant ninth = new Participant(1L, "ninth", schedule.nextColorIndex());
        schedule.addParticipant(ninth);
        Participant tenth = new Participant(1L, "tenth", schedule.nextColorIndex());
        schedule.addParticipant(tenth);

        assertThat(ninth.getColorHex()).isEqualTo("#92400E");
        assertThat(tenth.getColorHex()).isEqualTo("#0E7490");
    }

    @Test
    @DisplayName("addParticipant with duplicate name should throw (case-insensitive)")
    void addParticipant_duplicateName_throws() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.addParticipant(new Participant(1L, "Alice", 0));

        assertThatThrownBy(() -> schedule.addParticipant(new Participant(1L, "alice", 1)))
                .isInstanceOf(DuplicateParticipantException.class);
    }

    @Test
    @DisplayName("hasAvailabilityOn: 참여자 중 한 명이라도 저장한 날이면 true, 아니면 false")
    void hasAvailabilityOn_anyParticipantSelection() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        Participant alice = new Participant(1L, "Alice", 0);
        alice.updateSelections(List.of(5, 12), schedule.getTotalDays());
        Participant bob = new Participant(1L, "Bob", 1);
        bob.updateSelections(List.of(20), schedule.getTotalDays());
        schedule.addParticipant(alice);
        schedule.addParticipant(bob);

        assertThat(schedule.hasAvailabilityOn(12)).isTrue();
        assertThat(schedule.hasAvailabilityOn(20)).isTrue();
        assertThat(schedule.hasAvailabilityOn(13)).isFalse();
    }

    @Test
    @DisplayName("새 일정은 설명이 없다")
    void newSchedule_hasNoDescription() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.getDescription()).isNull();
    }

    @Test
    @DisplayName("changeDescription 은 앞뒤 공백을 잘라 저장하고 줄바꿈은 유지한다")
    void changeDescription_stripsAndKeepsLineBreaks() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        schedule.changeDescription("  회의 60분\n안건: https://notion.so/a  ");

        assertThat(schedule.getDescription()).isEqualTo("회의 60분\n안건: https://notion.so/a");
    }

    @Test
    @DisplayName("changeDescription 에 빈 값이나 null 을 주면 설명이 지워진다")
    void changeDescription_blankOrNull_clears() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.changeDescription("메모");

        schedule.changeDescription("   ");
        assertThat(schedule.getDescription()).isNull();

        schedule.changeDescription("메모");
        schedule.changeDescription(null);
        assertThat(schedule.getDescription()).isNull();
    }

    @Test
    @DisplayName("설명은 500자까지 허용하고 넘으면 거부한다")
    void changeDescription_lengthLimit() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        schedule.changeDescription("가".repeat(Schedule.MAX_DESCRIPTION_LENGTH));
        assertThat(schedule.getDescription()).hasSize(500);

        assertThatThrownBy(() -> schedule.changeDescription("가".repeat(Schedule.MAX_DESCRIPTION_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("hasAvailabilityOn: 참여자가 없으면 어떤 날도 false")
    void hasAvailabilityOn_noParticipants_false() {
        Schedule schedule = new Schedule("test-user", 2025, 12);

        assertThat(schedule.hasAvailabilityOn(1)).isFalse();
    }
}
