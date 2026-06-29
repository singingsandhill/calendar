package me.singingsandhill.calendar.common.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;

/**
 * Sitemap 생성 결과에 xhtml:link hreflang 엔트리가 공개 SEO 페이지에만 포함되는지 검증한다.
 */
class SitemapServiceHreflangTest {

    private static final String BASE_URL = "https://example.test";

    private SitemapService service;

    @BeforeEach
    void setUp() {
        // 선택적 의존성은 모두 null → 인기 데이터 없음 → /insights/trends 는 sitemap 에서 제외
        // (SeoService 가 noindex 메타로 응답하므로 sitemap 광고는 모순).
        // RunRepository 는 sitemap 에서 Runner 가 완전 제거된 이후 더 이상 의존성으로 받지 않는다.
        service = new SitemapService(BASE_URL, null, null, null);
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
    @DisplayName("Runner 경로는 sitemap 에서 완전히 제외된다 (AdSense 사이트 테마 정합성)")
    void runnerPathsAreNotInSitemap() {
        String xml = service.generateSitemapXml();

        // Runner URLs 는 어떤 형태로도 sitemap 에 나타나지 않아야 한다.
        // (컨트롤러는 noindex 메타로 색인을 차단하므로 sitemap 에서도 제외해 신호 일관성을 유지)
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners?lang=en</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners/runs</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners/members</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/runners/announce</loc>");
    }

    @Test
    @DisplayName("/about 페이지가 sitemap 에 양방향으로 포함된다")
    void aboutPageIsBilingualInSitemap() {
        String xml = service.generateSitemapXml();

        assertThat(xml).contains("<loc>" + BASE_URL + "/about</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/about?lang=en</loc>");
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"ko\" href=\"" + BASE_URL + "/about\"/>");
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"" + BASE_URL + "/about?lang=en\"/>");
    }

    @Test
    @DisplayName("인기 데이터 없을 때 — 공개 페이지 12개 × 2 url × 3 alt = 72개 (insights/trends 제외)")
    void hreflangEntryCountReasonable() {
        String xml = service.generateSitemapXml();
        int count = xml.split("<xhtml:link", -1).length - 1;
        // setUp 의 null 리포지토리로 인해 /insights/trends 는 제외.
        // 공개 양방향 엔트리 12개:
        //   home, guide, about, privacy, terms, faq, date-diff,
        //   use-cases x 5 (friend, team, travel, study, club-activity)
        // 각 엔트리는 ko/en 두 개 url, 각 url 은 3개 hreflang = 12 * 2 * 3 = 72
        assertThat(count).isEqualTo(12 * 2 * 3);
    }

    @Test
    @DisplayName("인기 데이터 없으면 /insights/trends 는 sitemap 에서 제외된다 (noindex 와 모순 회피)")
    void insightsTrendsExcludedWhenNoData() {
        String xml = service.generateSitemapXml();

        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/insights/trends</loc>");
        assertThat(xml).doesNotContain("<loc>" + BASE_URL + "/insights/trends?lang=en</loc>");
    }

    @Test
    @DisplayName("Location/Menu 활동이 존재하면 /insights/trends 가 sitemap 에 양방향으로 포함된다")
    void insightsTrendsIncludedWhenDataPresent() {
        LocationRepository locationRepo = mock(LocationRepository.class);
        MenuRepository menuRepo = mock(MenuRepository.class);
        LocalDateTime activity = LocalDateTime.of(2026, 1, 15, 10, 30);
        when(locationRepo.findLatestActivity()).thenReturn(Optional.of(activity));
        when(menuRepo.findLatestActivity()).thenReturn(Optional.empty());

        SitemapService withData = new SitemapService(BASE_URL, null, locationRepo, menuRepo);
        String xml = withData.generateSitemapXml();

        assertThat(xml).contains("<loc>" + BASE_URL + "/insights/trends</loc>");
        assertThat(xml).contains("<loc>" + BASE_URL + "/insights/trends?lang=en</loc>");
        assertThat(xml).contains("<xhtml:link rel=\"alternate\" hreflang=\"ko\" href=\"" + BASE_URL + "/insights/trends\"/>");
        // lastmod 은 활동 시각 기반이어야 함 (KST offset)
        assertThat(xml).contains("<lastmod>2026-01-15T10:30:00+09:00</lastmod>");
    }

    @Test
    @DisplayName("lastmod 은 ISO 8601 풀 정밀도 형식이어야 한다 — 같은 날 두 번 갱신 시 신호 손실 방지")
    void lastmodIsIsoOffsetDateTime() {
        String xml = service.generateSitemapXml();
        // YYYY-MM-DDTHH:mm:ss[.SSS]±HH:mm 형식이어야 함 (LocalDate 의 YYYY-MM-DD 단독 X)
        assertThat(xml).containsPattern("<lastmod>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(xml).doesNotContainPattern("<lastmod>\\d{4}-\\d{2}-\\d{2}</lastmod>");
    }

    @Test
    @DisplayName("같은 빌드/입력에 대해 sitemap.xml 은 결정적(deterministic)이다 — LocalDate.now() 같은 변동 신호 금지")
    void sitemapIsDeterministicAcrossCalls() {
        String first = service.generateSitemapXml();
        String second = service.generateSitemapXml();
        // 데이터/리포지토리 변경 없이 두 번 호출하면 lastmod 포함 동일해야 함
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("XML 생성된 sitemap 은 well-formed XML 이어야 한다")
    void sitemapIsWellFormedXml() throws Exception {
        String xml = service.generateSitemapXml();
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("urlset");
    }

    @Test
    @DisplayName("escapeXml 은 5개 미리 정의된 엔티티(& < > \" ') 를 모두 escape 한다")
    void xmlEscapeHandlesAllPredefinedEntities() {
        assertThat(SitemapService.escapeXml("a&b")).isEqualTo("a&amp;b");
        assertThat(SitemapService.escapeXml("a<b>c")).isEqualTo("a&lt;b&gt;c");
        assertThat(SitemapService.escapeXml("a\"b'c")).isEqualTo("a&quot;b&apos;c");
        // 예: UTM + lang 쿼리가 결합된 URL — 그대로 두면 invalid XML
        assertThat(SitemapService.escapeXml("https://example.test/?utm=x&lang=en"))
                .isEqualTo("https://example.test/?utm=x&amp;lang=en");
    }

    @Test
    @DisplayName("모든 <loc> 의 URL 은 https 로 시작하고 절대 경로다 — 색인 일관성")
    void allLocUrlsAreAbsoluteHttps() {
        String xml = service.generateSitemapXml();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<loc>([^<]+)</loc>").matcher(xml);
        while (m.find()) {
            String loc = m.group(1);
            assertThat(loc).startsWith("https://");
        }
    }

    @Test
    @DisplayName("hreflang reciprocal — 모든 ko URL 블록은 en alternate 를 포함하고 그 반대도 성립한다")
    void hreflangIsReciprocal() {
        String xml = service.generateSitemapXml();
        // bilingual 엔트리는 hreflang ko + en + x-default 를 항상 함께 가져야 함
        java.util.regex.Matcher urlBlocks = java.util.regex.Pattern.compile(
                "<url>(.*?)</url>", java.util.regex.Pattern.DOTALL).matcher(xml);
        while (urlBlocks.find()) {
            String block = urlBlocks.group(1);
            boolean hasKo = block.contains("hreflang=\"ko\"");
            boolean hasEn = block.contains("hreflang=\"en\"");
            boolean hasXDefault = block.contains("hreflang=\"x-default\"");
            // hreflang 이 하나라도 있으면 셋 다 있어야 함
            if (hasKo || hasEn || hasXDefault) {
                assertThat(hasKo).as("ko alt missing in block: " + block).isTrue();
                assertThat(hasEn).as("en alt missing in block: " + block).isTrue();
                assertThat(hasXDefault).as("x-default alt missing in block: " + block).isTrue();
            }
        }
    }
}
