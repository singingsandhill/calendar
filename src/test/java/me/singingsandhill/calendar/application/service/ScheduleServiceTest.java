package me.singingsandhill.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

import me.singingsandhill.calendar.application.exception.DuplicateScheduleException;
import me.singingsandhill.calendar.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.domain.schedule.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private OwnerRepository ownerRepository;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, ownerRepository);
    }

    @Test
    @DisplayName("getSchedule should return schedule when found")
    void getSchedule_existingSchedule_returnsSchedule() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        Schedule result = scheduleService.getSchedule(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getSchedule should throw exception when not found")
    void getSchedule_notFound_throwsException() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getSchedule(999L))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    @DisplayName("getScheduleByOwnerAndYearMonth should return schedule when found")
    void getScheduleByOwnerAndYearMonth_existingSchedule_returnsSchedule() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12))
                .thenReturn(Optional.of(schedule));

        Schedule result = scheduleService.getScheduleByOwnerAndYearMonth("test-user", 2025, 12);

        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("createSchedule should create schedule successfully")
    void createSchedule_validInput_createsSchedule() {
        when(ownerRepository.existsById("test-user")).thenReturn(true);
        when(scheduleRepository.existsByOwnerIdAndYearMonth(anyString(), anyInt(), anyInt()))
                .thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        Schedule result = scheduleService.createSchedule("test-user", 2025, 12, null);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOwnerId()).isEqualTo("test-user");
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("createSchedule should throw exception for duplicate")
    void createSchedule_duplicate_throwsException() {
        when(ownerRepository.existsById("test-user")).thenReturn(true);
        when(scheduleRepository.existsByOwnerIdAndYearMonth("test-user", 2025, 12))
                .thenReturn(true);

        assertThatThrownBy(() -> scheduleService.createSchedule("test-user", 2025, 12, null))
                .isInstanceOf(DuplicateScheduleException.class);
    }

    @Test
    @DisplayName("deleteSchedule should delete schedule successfully")
    void deleteSchedule_existingSchedule_deletes() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12))
                .thenReturn(Optional.of(schedule));

        scheduleService.deleteSchedule("test-user", 2025, 12);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    @DisplayName("deleteSchedule should throw exception when not found")
    void deleteSchedule_notFound_throwsException() {
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.deleteSchedule("test-user", 2025, 12))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    @DisplayName("getSchedulesByOwnerId should return list of schedules")
    void getSchedulesByOwnerId_returnsSchedules() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        when(scheduleRepository.findAllByOwnerId("test-user")).thenReturn(List.of(schedule));

        List<Schedule> result = scheduleService.getSchedulesByOwnerId("test-user");

        assertThat(result).hasSize(1);
    }
}
