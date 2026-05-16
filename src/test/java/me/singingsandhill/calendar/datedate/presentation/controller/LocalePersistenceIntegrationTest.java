package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;

/**
 * Verifies that after a user enters with `?lang=en`, internal navigation and
 * server redirects preserve the lang query param so the URL bar (and therefore
 * the share/bookmark/back-button experience) stays in English.
 *
 * Default locale (ko) MUST NOT add `?lang=ko` to URLs — keeps canonical URLs clean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalePersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private OwnerService ownerService;

    @Test
    @DisplayName("?lang=en 진입 후 /start POST → 리다이렉트 URL 에 ?lang=en 유지")
    void postStart_preservesLangInRedirect() throws Exception {
        when(ownerService.getOrCreateOwner(anyString()))
                .thenReturn(new Owner("sweet-bear-55"));

        mockMvc.perform(post("/start")
                        .with(csrf())
                        .param("ownerId", "sweet-bear-55")
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sweet-bear-55?lang=en"));
    }

    @Test
    @DisplayName("기본 locale (ko) 에서 /start POST → 리다이렉트 URL 에 lang 쿼리 추가 안 됨")
    void postStart_defaultLocale_noLangQuery() throws Exception {
        when(ownerService.getOrCreateOwner(anyString()))
                .thenReturn(new Owner("sweet-bear-55"));

        mockMvc.perform(post("/start")
                        .with(csrf())
                        .param("ownerId", "sweet-bear-55"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sweet-bear-55"));
    }

    @Test
    @DisplayName("?lang=en 으로 /insights → /insights/trends 308 redirect 시 lang 유지")
    void insightsRoot_redirectPreservesLang() throws Exception {
        mockMvc.perform(get("/insights").param("lang", "en"))
                .andExpect(status().is(308))
                .andExpect(header().string("Location", "/insights/trends?lang=en"));
    }

    @Test
    @DisplayName("기본 locale (ko) 에서 /insights → /insights/trends 308 redirect, lang 쿼리 없음")
    void insightsRoot_defaultLocale_noLangQuery() throws Exception {
        mockMvc.perform(get("/insights"))
                .andExpect(status().is(308))
                .andExpect(header().string("Location", "/insights/trends"));
    }

    @Test
    @DisplayName("쿠키 lang=en 만 있어도 /start POST 리다이렉트가 lang=en 보존")
    void postStart_withLangCookie_preservesLang() throws Exception {
        when(ownerService.getOrCreateOwner(anyString()))
                .thenReturn(new Owner("sweet-bear-55"));

        mockMvc.perform(post("/start")
                        .with(csrf())
                        .param("ownerId", "sweet-bear-55")
                        .cookie(new Cookie("lang", "en")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sweet-bear-55?lang=en"));
    }

    @Test
    @DisplayName("?lang=en 으로 GET / → 응답 HTML 의 내부 링크가 ?lang=en 포함")
    void homeRender_internalLinksContainLang() throws Exception {
        MvcResult result = mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn();

        String bodyContent = extractBody(result.getResponse().getContentAsString());
        // 헤더 nav / 푸터 / CTA 등 내부 링크에 lang=en 부착
        assertThat(bodyContent).contains("/guide?lang=en");
        assertThat(bodyContent).contains("/insights/trends?lang=en");
        // 외부 도메인 링크는 영향 없음
        assertThat(bodyContent).contains("https://docs.google.com/forms");
        assertThat(bodyContent).doesNotContain("docs.google.com/forms?lang=en");
    }

    @Test
    @DisplayName("기본 locale (ko) GET / → 일반 nav 링크에 ?lang= 추가 안 됨")
    void homeRender_defaultLocale_linksClean() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String bodyContent = extractBody(result.getResponse().getContentAsString());
        // 일반 내부 nav 링크는 깨끗해야 함 (canonical URL 보존)
        assertThat(bodyContent).contains("href=\"/guide\"");
        assertThat(bodyContent).contains("href=\"/insights/trends\"");
        assertThat(bodyContent).doesNotContain("href=\"/guide?lang=");
        assertThat(bodyContent).doesNotContain("href=\"/insights/trends?lang=");
        // 단, 언어 토글 버튼만 예외적으로 ?lang=ko/en 포함 — 사용자가 명시적으로 전환할 때 사용
    }

    private static String extractBody(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        if (start < 0 || end < 0) return html;
        return html.substring(start, end);
    }
}
