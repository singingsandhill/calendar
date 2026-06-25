package me.singingsandhill.calendar.common.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;

/**
 * Sitemap 화이트리스트 가드.
 *
 * <p>{@link SitemapServiceHreflangTest} 가 개별 제외를 검증한다면, 이 테스트는
 * <em>정확 집합 일치</em> 를 검증한다 — 어떤 신규 라우트(UGC /{ownerId}, runners,
 * trading, stock, api 등)가 sitemap 에 새어 들어와도 즉시 실패한다. AdSense 리뷰
 * 관점에서 sitemap 은 "리뷰 안전 공개 콘텐츠 페이지" 의 단일 목록이어야 한다.
 */
class SitemapServiceWhitelistTest {

    private static final String BASE_URL = "https://example.test";

    private static final List<String> WHITELIST_PATHS = List.of(
            "/", "/guide", "/about", "/privacy", "/terms", "/faq",
            "/tools/date-diff",
            "/use-cases/friend-meetup", "/use-cases/team-meeting",
            "/use-cases/travel-planning", "/use-cases/study-group",
            "/use-cases/club-activity");

    private static Set<String> locs(String xml) {
        Matcher m = Pattern.compile("<loc>([^<]+)</loc>").matcher(xml);
        Set<String> out = new HashSet<>();
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private static Set<String> whitelist() {
        Set<String> expected = new HashSet<>();
        for (String path : WHITELIST_PATHS) {
            expected.add(BASE_URL + path);
            expected.add(BASE_URL + path + "?lang=en");
        }
        return expected;
    }

    @Test
    @DisplayName("sitemap 은 화이트리스트된 공개 URL 만 포함한다 (ownerId/runners/trading/stock/api 누출 차단)")
    void sitemapIsExactlyTheWhitelist() {
        SitemapService service = new SitemapService(BASE_URL, null, null, null);

        assertThat(locs(service.generateSitemapXml())).isEqualTo(whitelist());
    }

    @Test
    @DisplayName("인기 데이터가 있으면 /insights/trends 한 쌍만 추가된다")
    void withInsightsData_onlyTrendsAdded() {
        LocationRepository locationRepo = mock(LocationRepository.class);
        MenuRepository menuRepo = mock(MenuRepository.class);
        when(locationRepo.findLatestActivity())
                .thenReturn(Optional.of(LocalDateTime.of(2026, 1, 15, 10, 30)));
        when(menuRepo.findLatestActivity()).thenReturn(Optional.empty());

        SitemapService withData = new SitemapService(BASE_URL, null, locationRepo, menuRepo);

        Set<String> expected = whitelist();
        expected.add(BASE_URL + "/insights/trends");
        expected.add(BASE_URL + "/insights/trends?lang=en");
        assertThat(locs(withData.generateSitemapXml())).isEqualTo(expected);
    }
}
