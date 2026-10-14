package me.singingsandhill.calendar.datedate.application.service;

import java.time.LocalDateTime;
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

    private static final int DEFAULT_LIMIT = 5;

    /** 표수 내림차순, 동점이면 최근 등록 우선 — 화면에 보이는 표수와 순서가 어긋나지 않게 (ADR datedate/domain/0012) */
    private static final Comparator<AggregatedItem> BY_VOTES_THEN_RECENCY =
            Comparator.comparingInt(AggregatedItem::totalVotes).reversed()
                    .thenComparing(AggregatedItem::latestCreatedAt, Comparator.reverseOrder());

    /**
     * 노출 기준 — ADR datedate/domain/0006.
     * 최소 2표: 여러 명이 동의한 항목만 노출 (1인 제안·셀프 투표 1회로는 진입 불가).
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

        return aggregated.values().stream()
                .filter(this::isExposable)
                .sorted(BY_VOTES_THEN_RECENCY)
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

        return aggregated.values().stream()
                .filter(this::isExposable)
                .sorted(BY_VOTES_THEN_RECENCY)
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

    private record AggregatedItem(String name, String url, int totalVotes, LocalDateTime latestCreatedAt) {}
}
