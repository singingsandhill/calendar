package me.singingsandhill.calendar.datedate.domain.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * /guides 슬러그 SSOT 불변식 가드.
 *
 * <p>정확 집합은 {@code SitemapServiceWhitelistTest} 가 고정하므로 여기서는 세션마다
 * 변하지 않는 불변식만 검증한다 — 슬러그 형식, 유일성, 날짜 정합(게시≤수정≤오늘),
 * use-case 슬러그와의 네임스페이스 비충돌. 날짜는 사이트맵 lastmod·Article JSON-LD·
 * 바이라인 3곳의 단일 진실원이라 어긋나면 신뢰 신호 전체가 오염된다 (ADR common/seo/0003).
 */
class GuideSlugsTest {

    @Test
    @DisplayName("최소 1개 기사가 등록돼 있고 how-to-pick-a-date 를 포함한다")
    void hasArticles() {
        assertThat(GuideSlugs.ALL).isNotEmpty();
        assertThat(GuideSlugs.contains("how-to-pick-a-date")).isTrue();
        assertThat(GuideSlugs.find("how-to-pick-a-date")).isPresent();
        assertThat(GuideSlugs.find("does-not-exist")).isEmpty();
    }

    @Test
    @DisplayName("슬러그는 kebab-case 이며 유일하다")
    void slugsAreKebabCaseAndUnique() {
        Set<String> seen = new HashSet<>();
        for (GuideSlug g : GuideSlugs.ALL) {
            assertThat(g.slug()).matches("[a-z]+(-[a-z]+)*");
            assertThat(seen.add(g.slug())).as("duplicate slug: " + g.slug()).isTrue();
        }
        // slugs() 는 ALL 의 순서를 그대로 반영한다 (허브 카드 순서 = 게시 순서)
        assertThat(GuideSlugs.slugs())
                .containsExactlyElementsOf(GuideSlugs.ALL.stream().map(GuideSlug::slug).toList());
    }

    @Test
    @DisplayName("게시일 ≤ 수정일 ≤ 오늘 (미래 날짜 금지)")
    void datesAreCoherent() {
        LocalDate today = LocalDate.now();
        for (GuideSlug g : GuideSlugs.ALL) {
            assertThat(g.published()).as(g.slug() + " published").isBeforeOrEqualTo(g.modified());
            assertThat(g.modified()).as(g.slug() + " modified").isBeforeOrEqualTo(today);
        }
    }

    @Test
    @DisplayName("use-case 슬러그와 충돌하지 않는다")
    void doesNotCollideWithUseCaseSlugs() {
        for (GuideSlug g : GuideSlugs.ALL) {
            assertThat(UseCaseSlugs.ALL).doesNotContain(g.slug());
        }
    }
}
