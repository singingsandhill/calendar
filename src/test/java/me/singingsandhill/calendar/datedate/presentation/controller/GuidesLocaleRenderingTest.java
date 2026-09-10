package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import me.singingsandhill.calendar.datedate.domain.guide.GuideSlugs;

/**
 * /guides 기사 렌더링 가드 (AdSense 저가치 콘텐츠 3차 대응의 핵심 산출물 검증).
 *
 * <p>기사 본문 키는 자유형(free-form)이라 {@code UseCaseContentCompletenessTest} 식
 * 키 열거가 불가능하다 — 대신 렌더링된 본문의 분량 하한·로케일 마커·미해석 키 부재를
 * 고정한다. 섹션 하나가 조용히 빠지면(키 누락) 분량 하한이 잡고, 로케일 누수는 마커
 * 부재/혼입이 잡는다. 슬러그가 SSOT 에 추가됐는데 마커 등록이 없으면 즉시 실패한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuidesLocaleRenderingTest {

    /** 기사 분량 하한 — 목표 ko 4,000~5,500자 / en 1,200~1,800단어에서 크롬·마크업 손실을 감안한 값. */
    private static final int KO_MIN_VISIBLE_CHARS = 3_800;
    private static final int EN_MIN_VISIBLE_WORDS = 1_100;

    /** slug → {고유 ko 마커, 고유 en 마커}. 4개 기사 간 서로 겹치지 않는 문구여야 한다. */
    private static final Map<String, String[]> MARKERS = Map.of(
            "how-to-pick-a-date", new String[] {"리드타임", "lead time"},
            "scheduling-methods-compared", new String[] {"준비 비용", "spreadsheet"},
            "scheduling-etiquette", new String[] {"노쇼", "RSVP"},
            "group-poll-best-practices", new String[] {"가지치기", "anchoring"}
    );

    @Autowired
    private MockMvc mockMvc;

    static Stream<String> slugs() {
        return GuideSlugs.slugs().stream();
    }

    @ParameterizedTest(name = "[{index}] /guides/{0} (ko)")
    @MethodSource("slugs")
    @DisplayName("기사 ko — 200 + 전용 뷰 + 분량 하한 + 고유 마커 + 바이라인 + Article JSON-LD")
    void articleKorean(String slug) throws Exception {
        String[] markers = MARKERS.get(slug);
        assertThat(markers).as("MARKERS 에 %s 등록 필요 (SSOT 추가 시 함께)", slug).isNotNull();

        MvcResult result = mockMvc.perform(get("/guides/" + slug))
                .andExpect(status().isOk())
                .andExpect(view().name("guides/" + slug))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"ko\"");
        assertThat(html).contains("\"@type\": \"Article\"").contains("datePublished");

        String text = visibleText(html);
        assertThat(text.length())
                .as("%s ko 본문 분량 (실측 %d자)", slug, text.length())
                .isGreaterThanOrEqualTo(KO_MIN_VISIBLE_CHARS);
        assertThat(text).contains(markers[0]);
        assertThat(text).contains("2026");
        assertThat(text).doesNotContain("??guides").doesNotContain("??seo.guides");
    }

    @ParameterizedTest(name = "[{index}] /guides/{0} (en)")
    @MethodSource("slugs")
    @DisplayName("기사 en — 영어 본문 + 단어 수 하한 + ko 마커 부재 (로케일 누수 가드)")
    void articleEnglish(String slug) throws Exception {
        String[] markers = MARKERS.get(slug);
        assertThat(markers).isNotNull();

        MvcResult result = mockMvc.perform(get("/guides/" + slug).param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");

        String text = visibleText(html);
        int words = text.split("\\s+").length;
        assertThat(words)
                .as("%s en 본문 단어 수 (실측 %d)", slug, words)
                .isGreaterThanOrEqualTo(EN_MIN_VISIBLE_WORDS);
        assertThat(text.toLowerCase()).contains(markers[1].toLowerCase());
        assertThat(text).doesNotContain(markers[0]);
        assertThat(text).doesNotContain("??guides").doesNotContain("??seo.guides");
    }

    @Test
    @DisplayName("기사 ② 비교표 — .guides-table 이 헤더 5기준과 함께 렌더된다 (기사별 특수 블록 가드)")
    void methodsComparedRendersComparisonTable() throws Exception {
        MvcResult result = mockMvc.perform(get("/guides/scheduling-methods-compared"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("class=\"guides-table\"");
        String text = visibleText(html);
        assertThat(text).contains("준비 비용").contains("응답 부담").contains("익명성");
    }

    @Test
    @DisplayName("허브 /guides — 200 + 전 기사 링크 + 카드에 summary 노출 (고아 페이지·thin 허브 방지)")
    void hubListsAllArticles() throws Exception {
        MvcResult result = mockMvc.perform(get("/guides"))
                .andExpect(status().isOk())
                .andExpect(view().name("guides/index"))
                .andReturn();

        String body = visibleTextWithHrefs(result.getResponse().getContentAsString());
        for (String slug : GuideSlugs.slugs()) {
            assertThat(body).contains("/guides/" + slug);
        }
        // 카드가 제목만이 아니라 summary 본문까지 렌더하는지 (thin 허브 방지)
        assertThat(body).contains("감이 아니라 설계로");
    }

    @Test
    @DisplayName("허브 /guides?lang=en — 영어 렌더링")
    void hubEnglish() throws Exception {
        MvcResult result = mockMvc.perform(get("/guides").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("lang=\"en\"");
    }

    @Test
    @DisplayName("알 수 없는 guides 슬러그 → HTTP 404 + error/4xx (ADR datedate/domain/0008)")
    void unknownSlug404() throws Exception {
        mockMvc.perform(get("/guides/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/4xx"));
    }

    @Test
    @DisplayName("홈 푸터가 모든 guides 기사 링크를 노출한다 (GuideNavAdvice + footer-minimal)")
    void footerListsAllGuideArticles() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        for (String slug : GuideSlugs.slugs()) {
            assertThat(html).contains("/guides/" + slug);
        }
    }

    /** body 에서 script/style 블록과 태그를 제거한 가시 텍스트. */
    private static String visibleText(String html) {
        String body = extractBody(html);
        return body.replaceAll("(?s)<script.*?</script>", " ")
                .replaceAll("(?s)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** 링크 href 는 남기고 태그만 벗긴 본문 (허브의 기사 링크 검증용). */
    private static String visibleTextWithHrefs(String html) {
        String body = extractBody(html);
        return body.replaceAll("(?s)<script.*?</script>", " ");
    }

    private static String extractBody(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        if (start < 0 || end < 0) {
            return html;
        }
        return html.substring(start, end);
    }
}
