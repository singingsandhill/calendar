package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    private OwnerService ownerService;

    @BeforeEach
    void setUp() {
        ownerService = new OwnerService(ownerRepository, scheduleRepository);
    }

    @Test
    @DisplayName("getOrCreateOwner should return existing owner when found")
    void getOrCreateOwner_existingOwner_returnsOwner() {
        Owner existingOwner = new Owner("test-user");
        when(ownerRepository.findById("test-user")).thenReturn(Optional.of(existingOwner));

        Owner result = ownerService.getOrCreateOwner("test-user");

        assertThat(result.getOwnerId()).isEqualTo("test-user");
        verify(ownerRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateOwner should create new owner when not found")
    void getOrCreateOwner_newOwner_createsOwner() {
        when(ownerRepository.findById("new-user")).thenReturn(Optional.empty());
        when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

        Owner result = ownerService.getOrCreateOwner("new-user");

        assertThat(result.getOwnerId()).isEqualTo("new-user");
        verify(ownerRepository).save(any(Owner.class));
    }

    @Test
    @DisplayName("getOwner should return owner when found")
    void getOwner_existingOwner_returnsOwner() {
        Owner existingOwner = new Owner("test-user");
        when(ownerRepository.findById("test-user")).thenReturn(Optional.of(existingOwner));

        Owner result = ownerService.getOwner("test-user");

        assertThat(result).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("getOwner should return null when not found")
    void getOwner_nonExistingOwner_returnsNull() {
        when(ownerRepository.findById("unknown")).thenReturn(Optional.empty());

        Owner result = ownerService.getOwner("unknown");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("ownerExists should return true for existing owner")
    void ownerExists_existingOwner_returnsTrue() {
        when(ownerRepository.existsById("test-user")).thenReturn(true);

        boolean result = ownerService.ownerExists("test-user");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("ownerExists should return false for non-existing owner")
    void ownerExists_nonExistingOwner_returnsFalse() {
        when(ownerRepository.existsById("unknown")).thenReturn(false);

        boolean result = ownerService.ownerExists("unknown");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getOwnerSchedules should return list of schedules")
    void getOwnerSchedules_returnsSchedules() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        when(scheduleRepository.findAllByOwnerId("test-user")).thenReturn(List.of(schedule));

        List<Schedule> result = ownerService.getOwnerSchedules("test-user");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwnerId()).isEqualTo("test-user");
    }
}
