package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.dto.InsightsOverviewDto;
import me.singingsandhill.calendar.datedate.application.dto.PopularItemDto;
import me.singingsandhill.calendar.datedate.application.dto.ServiceStatsDto;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class InsightsService {

    private final ScheduleRepository scheduleRepository;
    private final ParticipantRepository participantRepository;
    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;
    private final PopularityService popularityService;

    public InsightsService(ScheduleRepository scheduleRepository,
                           ParticipantRepository participantRepository,
                           LocationRepository locationRepository,
                           MenuRepository menuRepository,
                           PopularityService popularityService) {
        this.scheduleRepository = scheduleRepository;
        this.participantRepository = participantRepository;
        this.locationRepository = locationRepository;
        this.menuRepository = menuRepository;
        this.popularityService = popularityService;
    }

    public InsightsOverviewDto getInsightsOverview() {
        long totalSchedules = scheduleRepository.count();
        long totalParticipants = participantRepository.count();
        long totalLocations = locationRepository.count();
        long totalMenus = menuRepository.count();
        long totalVotes = locationRepository.countAllVotes() + menuRepository.countAllVotes();

        List<PopularItemDto> topLocations = popularityService.getPopularLocations(1);
        List<PopularItemDto> topMenus = popularityService.getPopularMenus(1);

        PopularItemDto topLocation = topLocations.isEmpty() ? null : topLocations.get(0);
        PopularItemDto topMenu = topMenus.isEmpty() ? null : topMenus.get(0);

        return new InsightsOverviewDto(
                totalSchedules,
                totalParticipants,
                totalLocations,
                totalMenus,
                totalVotes,
                topLocation,
                topMenu
        );
    }

    public ServiceStatsDto getServiceStats() {
        long totalSchedules = scheduleRepository.count();
        long totalParticipants = participantRepository.count();
        long totalLocations = locationRepository.count();
        long totalMenus = menuRepository.count();
        long totalLocationVotes = locationRepository.countAllVotes();
        long totalMenuVotes = menuRepository.countAllVotes();

        double avgParticipants = totalSchedules > 0
                ? (double) totalParticipants / totalSchedules
                : 0;

        return new ServiceStatsDto(
                totalSchedules,
                totalParticipants,
                totalLocations,
                totalMenus,
                totalLocationVotes,
                totalMenuVotes,
                avgParticipants
        );
    }
}
