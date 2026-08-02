package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.dto.ServiceStatsDto;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

/**
 * 핵심 회귀: 장소/메뉴당 평균 투표가 템플릿에서 long/long 정수 나눗셈으로 계산돼
 * 1 미만 평균이 전부 0.0 으로 표시되던 문제. 산술을 서비스로 옮기고 double 로 계산한다.
 */
class InsightsServiceTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final ParticipantRepository participantRepository = mock(ParticipantRepository.class);
    private final LocationRepository locationRepository = mock(LocationRepository.class);
    private final MenuRepository menuRepository = mock(MenuRepository.class);
    private final PopularityService popularityService = mock(PopularityService.class);

    private final InsightsService service = new InsightsService(
            scheduleRepository, participantRepository, locationRepository, menuRepository, popularityService);

    private void givenCounts(long schedules, long participants,
                             long locations, long locationVotes,
                             long menus, long menuVotes) {
        when(scheduleRepository.count()).thenReturn(schedules);
        when(participantRepository.count()).thenReturn(participants);
        when(locationRepository.count()).thenReturn(locations);
        when(locationRepository.countAllVotes()).thenReturn(locationVotes);
        when(menuRepository.count()).thenReturn(menus);
        when(menuRepository.countAllVotes()).thenReturn(menuVotes);
    }

    @Test
    @DisplayName("투표 수가 등록 수보다 적어도 평균이 0 으로 잘리지 않는다 (운영 관측값 재현)")
    void averageVotesBelowOneIsNotTruncated() {
        givenCounts(10, 30, 229, 200, 87, 72);

        ServiceStatsDto stats = service.getServiceStats();

        assertThat(stats.avgVotesPerLocation()).isCloseTo(0.873, within(0.001));
        assertThat(stats.avgVotesPerMenu()).isCloseTo(0.827, within(0.001));
    }

    @Test
    @DisplayName("평균의 소수부가 보존된다 (정수 나눗셈이면 3.0 이 된다)")
    void averageVotesKeepsFractionalPart() {
        givenCounts(1, 1, 2, 7, 4, 10);

        ServiceStatsDto stats = service.getServiceStats();

        assertThat(stats.avgVotesPerLocation()).isEqualTo(3.5);
        assertThat(stats.avgVotesPerMenu()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("등록된 장소·메뉴가 0 건이면 평균은 0 이고 예외가 나지 않는다")
    void averageVotesIsZeroWhenNothingRegistered() {
        givenCounts(0, 0, 0, 0, 0, 0);

        ServiceStatsDto stats = service.getServiceStats();

        assertThat(stats.avgVotesPerLocation()).isZero();
        assertThat(stats.avgVotesPerMenu()).isZero();
        assertThat(stats.avgParticipantsPerSchedule()).isZero();
    }

    @Test
    @DisplayName("일정당 평균 참여자 수는 기존 동작을 유지한다")
    void averageParticipantsPerScheduleUnchanged() {
        givenCounts(4, 10, 0, 0, 0, 0);

        assertThat(service.getServiceStats().avgParticipantsPerSchedule()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("원시 카운트는 리포지토리 값을 그대로 전달한다")
    void rawCountsArePassedThrough() {
        givenCounts(10, 30, 229, 200, 87, 72);

        ServiceStatsDto stats = service.getServiceStats();

        assertThat(stats.totalSchedules()).isEqualTo(10);
        assertThat(stats.totalParticipants()).isEqualTo(30);
        assertThat(stats.totalLocations()).isEqualTo(229);
        assertThat(stats.totalLocationVotes()).isEqualTo(200);
        assertThat(stats.totalMenus()).isEqualTo(87);
        assertThat(stats.totalMenuVotes()).isEqualTo(72);
    }
}
