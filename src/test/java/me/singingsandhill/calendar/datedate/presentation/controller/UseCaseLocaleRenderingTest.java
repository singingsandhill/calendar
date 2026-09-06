package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * 신규 use-case 슬러그(club-activity) 렌더링 + 데이터 구동 푸터 가드.
 *
 * <p>AdSense "low value content" 대응으로 추가한 동호회(club-activity) use-case 가 양 로케일에서
 * 충실히 렌더링되고, 데이터 구동 푸터가 {@code UseCaseSlugs.ALL} 의 모든 슬러그(신규 포함)를
 * 노출하는지(고아 페이지 방지) 검증한다. 알 수 없는 슬러그는 소프트 404(홈 리다이렉트)가 아니라
 * HTTP 404 로 응답한다 (ADR datedate/domain/0008 — owner 404 와 같은 논리).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UseCaseLocaleRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /use-cases/club-activity (기본 ko) → 본문이 한국어 동호회 콘텐츠")
    void clubActivityKoreanBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/use-cases/club-activity"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"ko\"");
        String body = extractBody(html);
        assertThat(body).contains("동호회");
    }

    @Test
    @DisplayName("GET /use-cases/club-activity?lang=en → 본문이 영어")
    void clubActivityEnglishBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/use-cases/club-activity").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
        String body = extractBody(html);
        assertThat(body).containsIgnoringCase("club");
        assertThat(body).doesNotContain("동호회");
    }

    @Test
    @DisplayName("GET /use-cases (허브 인덱스) → 200 + 전체 슬러그 카드 (평면 doorway 구조 해소)")
    void hubIndexListsAllSlugs() throws Exception {
        MvcResult result = mockMvc.perform(get("/use-cases"))
                .andExpect(status().isOk())
                .andExpect(view().name("use-cases/index"))
                .andReturn();

        String body = extractBody(result.getResponse().getContentAsString());
        for (String slug : UseCaseSlugs.ALL) {
            assertThat(body).contains("/use-cases/" + slug);
        }
    }

    @Test
    @DisplayName("GET /use-cases?lang=en → 영어 본문")
    void hubIndexEnglishBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/use-cases").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
    }

    @Test
    @DisplayName("알 수 없는 use-case 슬러그 → HTTP 404 + error/4xx 뷰 (소프트 404 리다이렉트 금지)")
    void unknownSlug404() throws Exception {
        mockMvc.perform(get("/use-cases/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/4xx"));
    }

    @Test
    @DisplayName("홈 푸터는 UseCaseSlugs.ALL 의 모든 use-case 링크를 노출한다 (신규 club-activity 포함)")
    void footerListsAllUseCaseSlugs() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String body = extractBody(result.getResponse().getContentAsString());
        assertThat(body)
                .contains("/use-cases/friend-meetup")
                .contains("/use-cases/team-meeting")
                .contains("/use-cases/travel-planning")
                .contains("/use-cases/study-group")
                .contains("/use-cases/club-activity");
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
