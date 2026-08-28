package me.singingsandhill.calendar.runner.presentation.controller;

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

/**
 * runner 페이지 og:image 치수 선언 가드.
 *
 * <p>공유 스크래퍼(카카오·페이스북)는 og:image:width/height 선언을 신뢰하므로,
 * 선언 치수는 실제 이미지 파일과 일치해야 한다. runner 페이지의 ogImage 는
 * crew_logo.png (1280×720), datedate 페이지는 og-image.png (1490×780).
 * (2026-08-16 SEO 점검 §4-1 — head.html 이 1490×780 을 무조건 출력하던 회귀 방지)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunnerSeoRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /runners → og:image 치수가 crew_logo.png 실물(1280×720)과 일치")
    void runnersHome_ogImageDimensionsMatchCrewLogo() throws Exception {
        String html = mockMvc.perform(get("/runners"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("crew_logo.png");
        assertThat(html).contains("property=\"og:image:width\" content=\"1280\"");
        assertThat(html).contains("property=\"og:image:height\" content=\"720\"");
    }

    @Test
    @DisplayName("GET /runners/admin/login → og:image 치수가 crew_logo.png 실물(1280×720)과 일치")
    void adminLogin_ogImageDimensionsMatchCrewLogo() throws Exception {
        String html = mockMvc.perform(get("/runners/admin/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("crew_logo.png");
        assertThat(html).contains("property=\"og:image:width\" content=\"1280\"");
        assertThat(html).contains("property=\"og:image:height\" content=\"720\"");
    }

    @Test
    @DisplayName("GET / → 기본 og:image 치수는 og-image.png 실물(1490×780) 유지")
    void datedateHome_ogImageDimensionsKeepDefault() throws Exception {
        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("og-image.png");
        assertThat(html).contains("property=\"og:image:width\" content=\"1490\"");
        assertThat(html).contains("property=\"og:image:height\" content=\"780\"");
    }

    @Test
    @DisplayName("GET /runners → runners 전용 navbar fragment 가 렌더링된다")
    void runnersHome_rendersNavbarFragment() throws Exception {
        String html = mockMvc.perform(get("/runners"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("runners-navbar");
        assertThat(html).contains("97 Runners");
    }
}
