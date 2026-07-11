package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class RecapServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private ParticipantRepository participantRepository;

    private RecapService recapService;

    @BeforeEach
    void setUp() {
        recapService = new RecapService(appUserRepository, ownerRepository, scheduleRepository,
                userActivityRepository, participantRepository, FIXED);
    }

    private void stubUser() {
        when(appUserRepository.findById(42L)).thenReturn(Optional.of(
                new AppUser(42L, 12345L, "지수", null,
                        LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0))));
    }

    @Test
    @DisplayName("오너 계열: 해당 연도 일정 수·연인원·최다 요일·가장 바쁜 달·동행 TOP 을 집계한다")
    void aggregatesOwnerLine() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of(
                new Owner("my-crew", LocalDateTime.of(2026, 1, 1, 0, 0), List.of(), 42L)));

        // 2026-03: 그리드 시작 = 2026-03-01(일). index 2 = 3/2(월), index 9 = 3/9(월), index 3 = 3/3(화)
        Participant p1 = new Participant(1L, 10L, "민준", ParticipantColor.ofIndex(0),
                List.of(2, 9), LocalDateTime.of(2026, 3, 1, 0, 0));
        Participant p2 = new Participant(2L, 10L, "서연", ParticipantColor.ofIndex(1),
                List.of(2, 3), LocalDateTime.of(2026, 3, 1, 0, 0));
        Schedule mar2026 = new Schedule(10L, "my-crew", 2026, 3, 7,
                LocalDateTime.of(2026, 2, 20, 0, 0), List.of(p1, p2));
        Schedule dec2025 = new Schedule(11L, "my-crew", 2025, 12, 7,
                LocalDateTime.of(2025, 11, 20, 0, 0), List.of());
        when(scheduleRepository.findAllByOwnerId("my-crew")).thenReturn(List.of(mar2026, dec2025));
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.year()).isEqualTo(2026);
        assertThat(recap.nickname()).isEqualTo("지수");
        assertThat(recap.schedulesCreated()).isEqualTo(1);
        assertThat(recap.totalParticipants()).isEqualTo(2);
        assertThat(recap.topWeekday()).isEqualTo("MONDAY");
        assertThat(recap.busiestMonth()).isEqualTo(3);
        assertThat(recap.topCompanions()).containsExactly("민준", "서연");
        assertThat(recap.empty()).isFalse();
    }

    @Test
    @DisplayName("활동 계열: 참여 일정 수(distinct)·선택 날짜 합·투표 TOP3 을 집계한다")
    void aggregatesActivityLine() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of());
        LocalDateTime when1 = LocalDateTime.of(2026, 5, 1, 12, 0);
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of(
                        new UserActivity(1L, 42L, ActivityType.PARTICIPATION, 20L, 100L, "지수", when1),
                        new UserActivity(2L, 42L, ActivityType.PARTICIPATION, 20L, 101L, "지수", when1),
                        new UserActivity(3L, 42L, ActivityType.LOCATION_VOTE, 20L, 200L, "성수 카페", when1),
                        new UserActivity(4L, 42L, ActivityType.LOCATION_VOTE, 21L, 201L, "성수 카페", when1),
                        new UserActivity(5L, 42L, ActivityType.MENU_VOTE, 20L, 300L, "마라탕", when1)));
        when(participantRepository.findById(100L)).thenReturn(Optional.of(
                new Participant(100L, 20L, "지수", ParticipantColor.ofIndex(0), List.of(1, 2, 3),
                        when1)));
        when(participantRepository.findById(101L)).thenReturn(Optional.empty());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.participationCount()).isEqualTo(1);
        assertThat(recap.daysSelected()).isEqualTo(3);
        assertThat(recap.topLocations()).containsExactly("성수 카페");
        assertThat(recap.topMenus()).containsExactly("마라탕");
        assertThat(recap.empty()).isFalse();
    }

    @Test
    @DisplayName("기록이 전혀 없으면 empty=true")
    void emptyRecapWhenNoData() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of());
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of());

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.empty()).isTrue();
        assertThat(recap.topWeekday()).isNull();
        assertThat(recap.busiestMonth()).isNull();
    }

    @Test
    @DisplayName("미연결 오너에서 로그인 생성한 일정도 SCHEDULE_CREATED 활동으로 집계된다")
    void countsScheduleCreatedActivityForUnlinkedOwner() {
        stubUser();
        when(ownerRepository.findAllByUserId(42L)).thenReturn(List.of());
        LocalDateTime when1 = LocalDateTime.of(2026, 5, 1, 12, 0);
        when(userActivityRepository.findAllByUserIdAndOccurredAtBetween(eq(42L), any(), any()))
                .thenReturn(List.of(
                        new UserActivity(1L, 42L, ActivityType.SCHEDULE_CREATED, 30L, 30L, "unlinked-crew", when1),
                        new UserActivity(2L, 42L, ActivityType.SCHEDULE_CREATED, 30L, 30L, "unlinked-crew", when1),
                        new UserActivity(3L, 42L, ActivityType.SCHEDULE_CREATED, 31L, 31L, "unlinked-crew", when1)));

        RecapDto recap = recapService.buildRecap(42L, 2026);

        assertThat(recap.schedulesCreated()).isEqualTo(2);
        assertThat(recap.empty()).isFalse();
    }

    @Test
    @DisplayName("연도 범위(2024~현재) 밖이면 InvalidRecapYearException")
    void rejectsOutOfRangeYear() {
        assertThatThrownBy(() -> recapService.buildRecap(42L, 2023))
                .isInstanceOf(InvalidRecapYearException.class);
        assertThatThrownBy(() -> recapService.buildRecap(42L, 2027))
                .isInstanceOf(InvalidRecapYearException.class);
    }
}
