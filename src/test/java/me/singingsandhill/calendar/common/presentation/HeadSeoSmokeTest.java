package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * head(seo) 렌더링 스모크 가드.
 *
 * <p>{@code fragments/head.html} 은 {@code ${seo.*()}} 를 null 가드 없이 호출하므로,
 * 컨트롤러가 {@code seo} 모델 속성을 빠뜨리면 SpEL 예외 → 500 이 난다 (2026-08-16
 * SEO 점검 §4-3). 이 테스트는 head(seo) 를 쓰는 무인증 도달 가능 전 라우트를 실제
 * 렌더링해 그 회귀를 잡는다.
 *
 * <p>단언 2개의 역할: ① 5xx 미만 — seo 누락 시 MvcExceptionHandler 가 error/5xx 를
 * 500 으로 렌더링하므로 여기서 걸린다. ② og:site_name 마커 — error 페이지 인라인
 * head 에는 없고 공용 head(seo) 에만 있으므로, 라우트가 조용히 에러 뷰로 빠진
 * 경우(4xx 포함)를 잡는다. 인증 필요 라우트(/me, /recap/**)와 데이터 필요 라우트
 * (run/member 상세)는 각자의 컨트롤러 테스트가 렌더링을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HeadSeoSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "GET {0} → head(seo) 렌더링, 5xx 아님")
    @ValueSource(strings = {
            "/",
            "/guide",
            "/about",
            "/privacy",
            "/terms",
            "/faq",
            "/tools/date-diff",
            "/insights/trends",
            "/use-cases/friend-meetup",
            "/use-cases/team-meeting",
            "/use-cases/travel-planning",
            "/use-cases/study-group",
            "/use-cases/club-activity",
            "/guides",
            "/guides/how-to-pick-a-date",
            "/login",
            "/smoke-owner-guard",          // 미존재 owner: 404 + 빈 대시보드 렌더 (ADR datedate/domain/0004)
            "/smoke-owner-guard/2026/8",   // 미존재 일정: schedule/create 렌더
            "/runners",
            "/runners/runs",
            "/runners/members",
            "/runners/announce",
            "/runners/runs/new",
            "/runners/admin/login",
    })
    void headFragmentRendersWithSeo(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("GET %s 가 5xx — 대개 컨트롤러의 seo 모델 속성 누락", path)
                .isLessThan(500);
        assertThat(result.getResponse().getContentAsString())
                .as("GET %s 응답에 head(seo) 마커(og:site_name) 없음 — 에러 뷰로 빠졌거나 head 미사용", path)
                .contains("property=\"og:site_name\" content=\"DateDate\"");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("keywords 미설정(noindex) 페이지는 빈 keywords 메타를 내지 않는다")
    void noindexPage_omitsEmptyKeywordsMeta() throws Exception {
        String login = mockMvc.perform(get("/login"))
                .andReturn().getResponse().getContentAsString();
        assertThat(login).doesNotContain("name=\"keywords\"");

        String home = mockMvc.perform(get("/"))
                .andReturn().getResponse().getContentAsString();
        assertThat(home).contains("name=\"keywords\"");
    }
}
