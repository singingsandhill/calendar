package me.singingsandhill.calendar.common.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sitemap 생성 결과에 xhtml:link hreflang 엔트리가 공개 SEO 페이지에만 포함되는지 검증한다.
 */
class SitemapServiceHreflangTest {

    private static final String BASE_URL = "https://example.test";

    private SitemapService service;

    @BeforeEach
    void setUp() {
        // RunRepository 는 선택적 의존성이므로 null 로 주입 → runner 엔트리는 repo 없으면 자동 제외
        service = new SitemapService(BASE_URL, null);
    }

    @Test
    @DisplayName("sitemap.xml 은 xhtml 네임스페이스를 포함한다")
    void xhtmlNamespaceIsDeclared() {
        String xml = service.generateSitemapXml();
        assertThat(xml).contains("xmlns:xhtml=\"http://www.w3.org/1999/xhtml\"");
    }

    @Test
    @DisplayName("공개 SEO 페이지는 한/영 양쪽 <loc> 이 포함되고 각각 3개의 xhtml:link 를 갖는다")
    void publicPagesHaveBilingualUrls() {
        String xml = service.generateSitemapXml();

        // 한국어 URL
        assertThat(xml).contains("<loc>" + BASE_URL + "/guide</loc>");
        // 영어 URL
        assertThat(xml).contains("<loc>" + BASE_URL + "/guide?lang=en</loc>");
        // hreflang 대체 링크 존재
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"ko\" href=\"" + BASE_URL + "/guide\"/>");
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"" + BASE_URL + "/guide?lang=en\"/>");
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"" + BASE_URL + "/guide\"/>");
    }

    @Test
    @DisplayName("Use-case 및 도구 페이지도 양방향 엔트리를 갖는다")
    void useCaseAndToolsBilingual() {
        String xml = service.generateSitemapXml();

        assertThat(xml).contains("<loc>" + BASE_URL + "/use-cases/friend-meetup</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/use-cases/friend-meetup?lang=en</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/tools/date-diff</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/tools/date-diff?lang=en</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/faq</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/faq?lang=en</loc>");
    }

    @Test
    @DisplayName("Runner 경로는 한국어 전용이며 xhtml:link 를 포함하지 않는다")
    void runnerPathsAreKoreanOnly() {
        String xml = service.generateSitemapXml();

        // Runner URLs 는 한 번씩만 등장하고 ?lang=en 변종이 없어야 한다
        assertThat(xml).contains("<loc>" + BASE_URL + "/runners</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners?lang=en</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners/runs?lang=en</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners/members?lang=en</loc>");
    }

    @Test
    @DisplayName("공개 페이지마다 양방향이면 xhtml:link 가 생긴다 — 공개 페이지 11개 × 2 url × 3 alt = 66개 이상")
    void hreflangEntryCountReasonable() {
        String xml = service.generateSitemapXml();
        int count = xml.split("<xhtml:link", -1).length - 1;
        // 공개 양방향 엔트리 11개(home, guide, privacy, terms, insights/trends, 4 use-cases, faq, date-diff)
        // 각 엔트리는 ko/en 두 개 url, 각 url 은 3개 hreflang = 11 * 2 * 3 = 66
        assertThat(count).isEqualTo(11 * 2 * 3);
    }
}
