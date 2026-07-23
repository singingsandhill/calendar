package me.singingsandhill.calendar.datedate.application.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.dto.PopularItemDto;
import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;

@Service
@Transactional(readOnly = true)
public class PopularityService {

    private static final int MAX_DAYS = 30;
    private static final double TIME_WEIGHT_MAX = 5.0;
    private static final int DEFAULT_LIMIT = 5;

    /**
     * 노출 기준 — ADR datedate/domain/0006.
     * 최소 2표: 여러 명이 동의한 항목만 노출 (0표 항목이 recency 보너스만으로 랭킹 진입 차단).
     * 블록리스트: 명백한 비속어 토큰만 — 자모 변형 우회는 막지 못하는 best-effort.
     */
    private static final int MIN_EXPOSURE_VOTES = 2;
    private static final List<String> BLOCKED_TOKENS = List.of(
            "시발", "씨발", "ㅅㅂ", "ㅆㅂ", "병신", "지랄", "좆", "존나", "ㅈㄴ", "썅", "개새끼"
    );

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
                .filter(this::isExposable)
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
                .filter(this::isExposable)
                .sorted(Comparator.comparingDouble((AggregatedItem item) ->
                        calculatePopularityScore(item.totalVotes, item.latestCreatedAt, now))
                        .reversed())
                .limit(limit)
                .map(item -> new PopularItemDto(item.name, item.url, item.totalVotes, item.latestCreatedAt))
                .collect(Collectors.toList());
    }

    /** 집계 합산 후 판정 — 동명 1표+1표는 2표로 통과, 블록리스트는 표수와 무관하게 제외 */
    private boolean isExposable(AggregatedItem item) {
        if (item.totalVotes < MIN_EXPOSURE_VOTES) {
            return false;
        }
        String normalized = item.name.toLowerCase().replaceAll("\\s+", "");
        return BLOCKED_TOKENS.stream().noneMatch(normalized::contains);
    }

    private double calculatePopularityScore(int voteCount, LocalDateTime createdAt, LocalDateTime now) {
        long daysBetween = ChronoUnit.DAYS.between(createdAt, now);
        double timeWeight = Math.max(0, (MAX_DAYS - daysBetween) / (double) MAX_DAYS * TIME_WEIGHT_MAX);
        return voteCount + timeWeight;
    }

    private record AggregatedItem(String name, String url, int totalVotes, LocalDateTime latestCreatedAt) {}
}
