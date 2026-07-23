package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.dto.PopularItemDto;
import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;

/**
 * 노출 기준(최소 2표 + 비속어 블록리스트) — ADR datedate/domain/0006.
 * 핵심 회귀: 0표 항목이 recency 보너스만으로 홈 인기 순위에 진입하던 문제.
 */
class PopularityServiceTest {

    private final LocationRepository locationRepository = mock(LocationRepository.class);
    private final MenuRepository menuRepository = mock(MenuRepository.class);
    private final PopularityService service = new PopularityService(locationRepository, menuRepository);

    private static final LocalDateTime NOW = LocalDateTime.now();

    private Location location(String name, int votes, LocalDateTime createdAt) {
        List<String> voters = IntStream.range(0, votes).mapToObj(i -> "voter" + i).toList();
        return new Location(1L, 1L, name, voters, createdAt);
    }

    private Menu menu(String name, String url, int votes, LocalDateTime createdAt) {
        List<String> voters = IntStream.range(0, votes).mapToObj(i -> "voter" + i).toList();
        return new Menu(1L, 1L, name, url, voters, createdAt);
    }

    @Test
    @DisplayName("0표·1표 항목은 최근 생성이어도 노출되지 않는다 (recency 보너스 단독 랭킹 차단)")
    void excludesItemsBelowVoteThreshold() {
        when(locationRepository.findAllOrderByPopularity()).thenReturn(List.of(
                location("아무도안뽑은곳", 0, NOW),
                location("한명만뽑은곳", 1, NOW)));

        assertThat(service.getPopularLocations()).isEmpty();
    }

    @Test
    @DisplayName("2표 이상 항목은 노출된다")
    void includesItemsAtVoteThreshold() {
        when(locationRepository.findAllOrderByPopularity()).thenReturn(List.of(
                location("강남역", 2, NOW.minusDays(40))));

        assertThat(service.getPopularLocations())
                .extracting(PopularItemDto::name)
                .containsExactly("강남역");
    }

    @Test
    @DisplayName("대소문자 동명 집계 합산으로 임계를 넘으면 노출된다 (1표+1표 → 2표)")
    void aggregatesSameNameBeforeThreshold() {
        when(locationRepository.findAllOrderByPopularity()).thenReturn(List.of(
                location("Gangnam", 1, NOW.minusDays(3)),
                location("gangnam", 1, NOW.minusDays(1))));

        List<PopularItemDto> result = service.getPopularLocations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalVotes()).isEqualTo(2);
    }

    @Test
    @DisplayName("비속어 블록리스트 항목은 표수가 충족돼도 노출되지 않는다 (공백 우회 포함)")
    void excludesBlockedNamesRegardlessOfVotes() {
        when(menuRepository.findAllOrderByPopularity()).thenReturn(List.of(
                menu("ㅈㄴ맛있는거", null, 5, NOW),
                menu("존나 매운 떡볶이", null, 4, NOW),
                menu("마라탕", null, 3, NOW)));

        assertThat(service.getPopularMenus())
                .extracting(PopularItemDto::name)
                .containsExactly("마라탕");
    }

    @Test
    @DisplayName("전부 기준 미달이면 빈 리스트를 반환한다 (InsightsService top null 경로)")
    void returnsEmptyWhenNothingQualifies() {
        when(locationRepository.findAllOrderByPopularity()).thenReturn(List.of(
                location("어디", 1, NOW)));
        when(menuRepository.findAllOrderByPopularity()).thenReturn(List.of(
                menu("뭐먹지", null, 0, NOW)));

        assertThat(service.getPopularLocations(1)).isEmpty();
        assertThat(service.getPopularMenus(1)).isEmpty();
    }

    @Test
    @DisplayName("필터 후 정렬(표수+최신 가중)과 limit 이 기존대로 동작한다")
    void sortsAndLimitsAfterFiltering() {
        when(locationRepository.findAllOrderByPopularity()).thenReturn(List.of(
                location("삼표집", 3, NOW.minusDays(40)),   // score 3 + 0
                location("십표집", 10, NOW.minusDays(40)),  // score 10 + 0
                location("일표집", 1, NOW),                  // 임계 미달
                location("오표집", 5, NOW.minusDays(40)))); // score 5 + 0

        assertThat(service.getPopularLocations(2))
                .extracting(PopularItemDto::name)
                .containsExactly("십표집", "오표집");
    }

    @Test
    @DisplayName("메뉴 URL 병합(첫 non-null 승리)은 필터 도입 후에도 유지된다")
    void keepsMenuUrlMergeBehaviour() {
        when(menuRepository.findAllOrderByPopularity()).thenReturn(List.of(
                menu("치킨", null, 1, NOW.minusDays(2)),
                menu("치킨", "https://baemin.example/chicken", 1, NOW)));

        List<PopularItemDto> result = service.getPopularMenus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).url()).isEqualTo("https://baemin.example/chicken");
        assertThat(result.get(0).totalVotes()).isEqualTo(2);
    }
}
