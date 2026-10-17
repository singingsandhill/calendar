package me.singingsandhill.calendar.datedate.domain.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeSlotTest {

    @Test
    @DisplayName("유효한 날짜·시간대로 생성하면 값이 그대로 보존되고 표는 0이다")
    void validTimeSlot_createsSuccessfully() {
        TimeSlot slot = new TimeSlot(1L, 20, 18 * 60, 21 * 60);

        assertThat(slot.getScheduleId()).isEqualTo(1L);
        assertThat(slot.getDayIndex()).isEqualTo(20);
        assertThat(slot.getStartMinute()).isEqualTo(1080);
        assertThat(slot.getEndMinute()).isEqualTo(1260);
        assertThat(slot.getVoteCount()).isZero();
    }

    @Test
    @DisplayName("23:30~24:00 처럼 자정에 끝나는 마지막 칸은 허용된다")
    void endAtMidnight_isAllowed() {
        TimeSlot slot = new TimeSlot(1L, 1, 1410, 1440);

        assertThat(slot.getEndMinute()).isEqualTo(1440);
    }

    @ParameterizedTest(name = "day={0}, start={1}, end={2}")
    @CsvSource({
            "0, 600, 660",     // dayIndex 는 1부터
            "1, 660, 660",     // 길이 0
            "1, 690, 660",     // 종료가 시작보다 이름 (자정 넘김 미지원)
            "1, 605, 660",     // 30분 단위 아님 (시작)
            "1, 600, 645",     // 30분 단위 아님 (종료)
            "1, -30, 60",      // 00:00 이전
            "1, 1410, 1470"    // 24:00 이후
    })
    @DisplayName("날짜·시간 범위가 규칙을 벗어나면 IllegalArgumentException")
    void invalidRange_throws(int dayIndex, int start, int end) {
        assertThatThrownBy(() -> new TimeSlot(1L, dayIndex, start, end))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("투표하면 투표자와 표 수가 늘어난다")
    void addVote_addsVoter() {
        TimeSlot slot = new TimeSlot(1L, 20, 1080, 1260);

        slot.addVote("민수");

        assertThat(slot.getVoters()).containsExactly("민수");
        assertThat(slot.getVoteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 이름(대소문자 무시)으로 두 번 투표하면 IllegalStateException")
    void addVote_duplicateIgnoringCase_throws() {
        TimeSlot slot = new TimeSlot(1L, 20, 1080, 1260);
        slot.addVote("Alice");

        assertThatThrownBy(() -> slot.addVote("alice"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("빈 이름으로는 투표할 수 없다")
    void addVote_blankName_throws() {
        TimeSlot slot = new TimeSlot(1L, 20, 1080, 1260);

        assertThatThrownBy(() -> slot.addVote(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("투표 취소는 대소문자를 무시하고 해당 투표자만 제거한다")
    void removeVote_ignoringCase_removesOnlyThatVoter() {
        TimeSlot slot = new TimeSlot(1L, 20, 1080, 1260);
        slot.addVote("Alice");
        slot.addVote("Bob");

        slot.removeVote("ALICE");

        assertThat(slot.getVoters()).containsExactly("Bob");
        assertThat(slot.getVoteCount()).isEqualTo(1);
    }
}
