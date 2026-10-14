package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

import me.singingsandhill.calendar.datedate.application.exception.DuplicateScheduleException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private OwnerService ownerService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, ownerService);
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
        when(ownerService.getOrCreateOwner("test-user")).thenReturn(new Owner("test-user"));
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
        when(ownerService.getOrCreateOwner("test-user")).thenReturn(new Owner("test-user"));
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

    @Test
    @DisplayName("updateSchedule 에 설명만 주면 설명을 바꿔 저장한다")
    void updateSchedule_descriptionOnly_savesDescription() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schedule result = scheduleService.updateSchedule("test-user", 2025, 12, null, " 회의 60분 ");

        assertThat(result.getDescription()).isEqualTo("회의 60분");
        assertThat(result.getWeeks()).isEqualTo(7);
        verify(scheduleRepository).save(schedule);
    }

    @Test
    @DisplayName("updateSchedule 에 주 수만 주면 설명은 그대로다")
    void updateSchedule_weeksOnly_keepsDescription() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.changeDescription("메모");
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schedule result = scheduleService.updateSchedule("test-user", 2025, 12, 5, null);

        assertThat(result.getWeeks()).isEqualTo(5);
        assertThat(result.getDescription()).isEqualTo("메모");
    }

    @Test
    @DisplayName("updateSchedule 에 바꿀 값이 없으면 저장하지 않는다")
    void updateSchedule_nothingToChange_doesNotSave() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        when(scheduleRepository.findByOwnerIdAndYearMonth("test-user", 2025, 12)).thenReturn(Optional.of(schedule));

        scheduleService.updateSchedule("test-user", 2025, 12, null, null);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }
}
