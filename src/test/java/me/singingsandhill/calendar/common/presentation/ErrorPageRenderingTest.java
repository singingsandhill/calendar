package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.RecapShareService;

/**
 * 에러 페이지(error/4xx·5xx) head 렌더링 가드.
 *
 * <p>에러 페이지는 공용 head(seo) 대신 전용 error-head fragment 를 쓴다 — 에러 템플릿은
 * MvcExceptionHandler 외에 Spring Boot 컨테이너 에러 경로(BasicErrorController)로도
 * 렌더링되는데 그 경로엔 seo 모델이 없기 때문. 이 테스트는 MvcExceptionHandler 경로로
 * 두 템플릿을 실제 렌더링해, 2026-08-16 SEO 점검 §4-5 가 지적한 공백(GTM 미계측·동기 폰트)이
 * 닫혀 있고 noindex 가 유지됨을 고정한다. (컨테이너 경로는 MockMvc 가 /error 포워딩을
 * 따라가지 않아 여기서 검증 불가 — 같은 템플릿을 공유하므로 커버리지는 동일.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecapShareService recapShareService;

    @Test
    @DisplayName("error/4xx → GTM 로더·noscript 폴백·비동기 폰트·noindex 렌더링")
    void errorPage4xx_rendersGtmAndAsyncFonts() throws Exception {
        when(recapShareService.getByToken("nope"))
                .thenThrow(new RecapShareNotFoundException("nope"));

        String html = mockMvc.perform(get("/recap/share/nope"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertErrorHead(html);
    }

    @Test
    @DisplayName("error/5xx → GTM 로더·noscript 폴백·비동기 폰트·noindex 렌더링")
    void errorPage5xx_rendersGtmAndAsyncFonts() throws Exception {
        when(recapShareService.getByToken("boom"))
                .thenThrow(new RuntimeException("boom"));

        String html = mockMvc.perform(get("/recap/share/boom"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertErrorHead(html);
    }

    private static void assertErrorHead(String html) {
        assertThat(html).contains("googletagmanager.com/gtm.js");
        assertThat(html).contains("ns.html?id=");
        assertThat(html).contains("as=\"style\" onload=");
        assertThat(html).contains("name=\"robots\" content=\"noindex\"");
    }
}
