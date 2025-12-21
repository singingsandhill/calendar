package me.singingsandhill.calendar.application.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.application.dto.PopularItemDto;
import me.singingsandhill.calendar.domain.location.Location;
import me.singingsandhill.calendar.domain.location.LocationRepository;
import me.singingsandhill.calendar.domain.menu.Menu;
import me.singingsandhill.calendar.domain.menu.MenuRepository;

@Service
@Transactional(readOnly = true)
public class PopularityService {

    private static final int MAX_DAYS = 30;
    private static final double TIME_WEIGHT_MAX = 5.0;
    private static final int DEFAULT_LIMIT = 5;

    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;

    public PopularityService(LocationRepository locationRepository,
                             MenuRepository menuRepository) {
        this.locationRepository = locationRepository;
        this.menuRepository = menuRepository;
    }

    public List<PopularItemDto> getPopularLocations() {
        return getPopularLocations(DEFAULT_LIMIT);
    }

    public List<PopularItemDto> getPopularLocations(int limit) {
        List<Location> all = locationRepository.findAllOrderByPopularity();
        return aggregateLocationsByName(all, limit);
    }

    public List<PopularItemDto> getPopularMenus() {
        return getPopularMenus(DEFAULT_LIMIT);
    }

    public List<PopularItemDto> getPopularMenus(int limit) {
        List<Menu> all = menuRepository.findAllOrderByPopularity();
        return aggregateMenusByName(all, limit);
    }

    private List<PopularItemDto> aggregateLocationsByName(List<Location> locations, int limit) {
        Map<String, AggregatedItem> aggregated = new HashMap<>();

        for (Location location : locations) {
            String key = location.getName().toLowerCase();
            aggregated.compute(key, (k, existing) -> {
                if (existing == null) {
                    return new AggregatedItem(
                            location.getName(),
                            null,
                            location.getVoteCount(),
                            location.getCreatedAt()
                    );
                } else {
                    return new AggregatedItem(
                            existing.name,
                            null,
                            existing.totalVotes + location.getVoteCount(),
                            existing.latestCreatedAt.isAfter(location.getCreatedAt())
                                    ? existing.latestCreatedAt
                                    : location.getCreatedAt()
                    );
                }
            });
        }

        LocalDateTime now = LocalDateTime.now();
        return aggregated.values().stream()
                .sorted(Comparator.comparingDouble((AggregatedItem item) ->
                        calculatePopularityScore(item.totalVotes, item.latestCreatedAt, now))
                        .reversed())
                .limit(limit)
                .map(item -> new PopularItemDto(item.name, item.url, item.totalVotes, item.latestCreatedAt))
                .collect(Collectors.toList());
    }

    private List<PopularItemDto> aggregateMenusByName(List<Menu> menus, int limit) {
        Map<String, AggregatedItem> aggregated = new HashMap<>();

        for (Menu menu : menus) {
            String key = menu.getName().toLowerCase();
            aggregated.compute(key, (k, existing) -> {
                if (existing == null) {
                    return new AggregatedItem(
                            menu.getName(),
                            menu.getUrl(),
                            menu.getVoteCount(),
                            menu.getCreatedAt()
                    );
                } else {
                    return new AggregatedItem(
                            existing.name,
                            existing.url != null ? existing.url : menu.getUrl(),
                            existing.totalVotes + menu.getVoteCount(),
                            existing.latestCreatedAt.isAfter(menu.getCreatedAt())
                                    ? existing.latestCreatedAt
                                    : menu.getCreatedAt()
                    );
                }
            });
        }

        LocalDateTime now = LocalDateTime.now();
        return aggregated.values().stream()
                .sorted(Comparator.comparingDouble((AggregatedItem item) ->
                        calculatePopularityScore(item.totalVotes, item.latestCreatedAt, now))
                        .reversed())
                .limit(limit)
                .map(item -> new PopularItemDto(item.name, item.url, item.totalVotes, item.latestCreatedAt))
                .collect(Collectors.toList());
    }

    private double calculatePopularityScore(int voteCount, LocalDateTime createdAt, LocalDateTime now) {
        long daysBetween = ChronoUnit.DAYS.between(createdAt, now);
        double timeWeight = Math.max(0, (MAX_DAYS - daysBetween) / (double) MAX_DAYS * TIME_WEIGHT_MAX);
        return voteCount + timeWeight;
    }

    private record AggregatedItem(String name, String url, int totalVotes, LocalDateTime latestCreatedAt) {}
}
