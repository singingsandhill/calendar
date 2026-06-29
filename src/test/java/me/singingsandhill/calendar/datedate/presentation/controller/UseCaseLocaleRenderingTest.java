package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 신규 use-case 슬러그(club-activity) 렌더링 + 데이터 구동 푸터 가드.
 *
 * <p>AdSense "low value content" 대응으로 추가한 동호회(club-activity) use-case 가 양 로케일에서
 * 충실히 렌더링되고, 데이터 구동 푸터가 {@code UseCaseSlugs.ALL} 의 모든 슬러그(신규 포함)를
 * 노출하는지(고아 페이지 방지) 검증한다. 알 수 없는 슬러그는 홈으로 리다이렉트한다.
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
    @DisplayName("알 수 없는 use-case 슬러그 → 홈으로 리다이렉트")
    void unknownSlugRedirectsHome() throws Exception {
        mockMvc.perform(get("/use-cases/does-not-exist"))
                .andExpect(status().is3xxRedirection());
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
