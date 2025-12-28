package me.singingsandhill.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.application.exception.DuplicateParticipantException;
import me.singingsandhill.calendar.application.exception.InvalidSelectionException;
import me.singingsandhill.calendar.application.exception.ParticipantLimitExceededException;
import me.singingsandhill.calendar.application.exception.ParticipantNotFoundException;
import me.singingsandhill.calendar.domain.participant.Participant;
import me.singingsandhill.calendar.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.domain.schedule.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    private ParticipantService participantService;

    @BeforeEach
    void setUp() {
        participantService = new ParticipantService(participantRepository, scheduleRepository);
    }

    @Test
    @DisplayName("addParticipant should add participant successfully")
    void addParticipant_validInput_addsParticipant() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(participantRepository.countByScheduleId(1L)).thenReturn(0);
        when(participantRepository.existsByScheduleIdAndName(1L, "Alice")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenAnswer(i -> {
            Participant p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Participant result = participantService.addParticipant(1L, "Alice");

        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getColorHex()).isNotNull();
    }

    @Test
    @DisplayName("addParticipant should throw exception when limit exceeded")
    void addParticipant_limitExceeded_throwsException() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(participantRepository.countByScheduleId(1L)).thenReturn(8);

        assertThatThrownBy(() -> participantService.addParticipant(1L, "Alice"))
                .isInstanceOf(ParticipantLimitExceededException.class);
    }

    @Test
    @DisplayName("addParticipant should throw exception for duplicate name")
    void addParticipant_duplicateName_throwsException() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(participantRepository.countByScheduleId(1L)).thenReturn(1);
        when(participantRepository.existsByScheduleIdAndName(1L, "Alice")).thenReturn(true);

        assertThatThrownBy(() -> participantService.addParticipant(1L, "Alice"))
                .isInstanceOf(DuplicateParticipantException.class);
    }

    @Test
    @DisplayName("deleteParticipant should delete participant successfully")
    void deleteParticipant_existingParticipant_deletes() {
        Participant participant = new Participant(1L, "Alice", 0);
        participant.setId(1L);
        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));

        participantService.deleteParticipant(1L);

        verify(participantRepository).delete(participant);
    }

    @Test
    @DisplayName("deleteParticipant should throw exception when not found")
    void deleteParticipant_notFound_throwsException() {
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.deleteParticipant(999L))
                .isInstanceOf(ParticipantNotFoundException.class);
    }

    @Test
    @DisplayName("updateSelections should update selections successfully")
    void updateSelections_validSelections_updates() {
        Participant participant = new Participant(1L, "Alice", 0);
        participant.setId(1L);
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

        Participant result = participantService.updateSelections(1L, List.of(1, 15, 25));

        assertThat(result.getSelections()).containsExactly(1, 15, 25);
    }

    @Test
    @DisplayName("updateSelections should throw exception for invalid day")
    void updateSelections_invalidDay_throwsException() {
        Participant participant = new Participant(1L, "Alice", 0);
        participant.setId(1L);
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        // 7주 확장 모드에서 totalDays는 49이므로 50 이상은 유효하지 않음
        assertThatThrownBy(() -> participantService.updateSelections(1L, List.of(50)))
                .isInstanceOf(InvalidSelectionException.class);
    }

    @Test
    @DisplayName("getParticipantsByScheduleId should return list of participants")
    void getParticipantsByScheduleId_returnsParticipants() {
        Participant participant = new Participant(1L, "Alice", 0);
        when(participantRepository.findAllByScheduleId(1L)).thenReturn(List.of(participant));

        List<Participant> result = participantService.getParticipantsByScheduleId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }
}
