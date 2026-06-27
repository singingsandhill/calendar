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
 * 법적 페이지 (privacy/terms) 로케일 렌더링 가드.
 *
 * <p>sitemap + hreflang 이 /privacy?lang=en, /terms?lang=en 을 영문 대체 페이지로
 * 광고하므로, 실제 본문도 영어로 렌더링되어야 한다. head 의 SEO 메타는 원래부터
 * 영어였으므로 (seo.privacy.* 키) 반드시 &lt;body&gt; 본문을 검증해야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PolicyPagesLocaleRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /privacy?lang=en → 본문이 영어, html lang=en")
    void privacyEnglishBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/privacy").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
        String body = extractBody(html);
        assertThat(body).contains("Information We Collect");
        assertThat(body).doesNotContain("수집하는 정보");
    }

    @Test
    @DisplayName("GET /privacy (기본 ko) → 본문이 한국어, html lang=ko")
    void privacyKoreanBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"ko\"");
        String body = extractBody(html);
        assertThat(body).contains("수집하는 정보");
        assertThat(body).doesNotContain("Information We Collect");
    }

    @Test
    @DisplayName("GET /terms?lang=en → 본문이 영어, html lang=en")
    void termsEnglishBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/terms").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
        String body = extractBody(html);
        assertThat(body).contains("Service Overview");
        assertThat(body).doesNotContain("서비스 개요");
    }

    @Test
    @DisplayName("GET /terms (기본 ko) → 본문이 한국어, html lang=ko")
    void termsKoreanBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("lang=\"ko\"");
        String body = extractBody(html);
        assertThat(body).contains("서비스 개요");
        assertThat(body).doesNotContain("Service Overview");
    }

    @Test
    @DisplayName("GET /guide → 트러블슈팅 섹션이 양 로케일에서 렌더링된다")
    void guideTroubleshootingRenders() throws Exception {
        MvcResult ko = mockMvc.perform(get("/guide"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(extractBody(ko.getResponse().getContentAsString()))
                .contains("guide-faq-1")
                .contains("자주 겪는 문제 해결");

        MvcResult en = mockMvc.perform(get("/guide").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(extractBody(en.getResponse().getContentAsString()))
                .contains("guide-faq-1")
                .contains("Troubleshooting common issues");
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
