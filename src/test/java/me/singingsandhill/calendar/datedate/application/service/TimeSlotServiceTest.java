package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateTimeSlotException;
import me.singingsandhill.calendar.datedate.application.exception.InvalidTimeSlotException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.TimeSlotNotFoundException;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlotRepository;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    private TimeSlotService timeSlotService;

    @BeforeEach
    void setUp() {
        timeSlotService = new TimeSlotService(timeSlotRepository, scheduleRepository);
    }

    /** 참여자 Alice 가 20일(인덱스)만 가능한 날로 저장한 일정 */
    private Schedule scheduleWithAliceOnDay20() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        Participant alice = new Participant(1L, "Alice", 0);
        alice.updateSelections(List.of(20), schedule.getTotalDays());
        schedule.addParticipant(alice);
        return schedule;
    }

    @Test
    @DisplayName("addTimeSlot: 누군가 저장한 날이면 후보를 저장해 반환한다")
    void addTimeSlot_selectedDay_saves() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithAliceOnDay20()));
        when(timeSlotRepository.existsByScheduleIdAndRange(1L, 20, 1080, 1200)).thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> {
            TimeSlot slot = i.getArgument(0);
            slot.setId(7L);
            return slot;
        });

        TimeSlot result = timeSlotService.addTimeSlot(1L, 20, 1080, 1200);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getScheduleId()).isEqualTo(1L);
        assertThat(result.getDayIndex()).isEqualTo(20);
        assertThat(result.getStartMinute()).isEqualTo(1080);
        assertThat(result.getEndMinute()).isEqualTo(1200);
    }

    @Test
    @DisplayName("addTimeSlot: 일정이 없으면 ScheduleNotFoundException")
    void addTimeSlot_scheduleMissing_throws() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeSlotService.addTimeSlot(1L, 20, 1080, 1200))
                .isInstanceOf(ScheduleNotFoundException.class);
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("addTimeSlot: 아무도 저장하지 않은 날이면 InvalidTimeSlotException(400) 이고 저장하지 않는다")
    void addTimeSlot_unselectedDay_throws() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithAliceOnDay20()));

        assertThatThrownBy(() -> timeSlotService.addTimeSlot(1L, 21, 1080, 1200))
                .isInstanceOf(InvalidTimeSlotException.class)
                .extracting("status").hasToString("400 BAD_REQUEST");
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("addTimeSlot: 같은 날짜·시간대 후보가 이미 있으면 DuplicateTimeSlotException(409)")
    void addTimeSlot_duplicate_throws() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithAliceOnDay20()));
        when(timeSlotRepository.existsByScheduleIdAndRange(1L, 20, 1080, 1200)).thenReturn(true);

        assertThatThrownBy(() -> timeSlotService.addTimeSlot(1L, 20, 1080, 1200))
                .isInstanceOf(DuplicateTimeSlotException.class)
                .extracting("status").hasToString("409 CONFLICT");
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("vote: 투표자를 추가해 저장한다")
    void vote_addsVoterAndSaves() {
        TimeSlot slot = new TimeSlot(7L, 1L, 20, 1080, 1200, List.of("Bob"), LocalDateTime.now());
        when(timeSlotRepository.findById(7L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> i.getArgument(0));

        TimeSlot result = timeSlotService.vote(7L, "Alice");

        assertThat(result.getVoters()).containsExactly("Bob", "Alice");
        verify(timeSlotRepository).save(slot);
    }

    @Test
    @DisplayName("unvote: 투표자를 제거해 저장한다")
    void unvote_removesVoterAndSaves() {
        TimeSlot slot = new TimeSlot(7L, 1L, 20, 1080, 1200, List.of("Alice", "Bob"), LocalDateTime.now());
        when(timeSlotRepository.findById(7L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> i.getArgument(0));

        TimeSlot result = timeSlotService.unvote(7L, "Alice");

        assertThat(result.getVoters()).containsExactly("Bob");
        verify(timeSlotRepository).save(slot);
    }

    @Test
    @DisplayName("vote/unvote: 후보가 없으면 TimeSlotNotFoundException(404)")
    void vote_missingSlot_throws() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeSlotService.vote(99L, "Alice"))
                .isInstanceOf(TimeSlotNotFoundException.class)
                .extracting("status").hasToString("404 NOT_FOUND");
        assertThatThrownBy(() -> timeSlotService.unvote(99L, "Alice"))
                .isInstanceOf(TimeSlotNotFoundException.class);
    }
}
