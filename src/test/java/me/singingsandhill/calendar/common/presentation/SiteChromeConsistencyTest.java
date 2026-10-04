package me.singingsandhill.calendar.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.datedate.domain.guide.GuideSlugs;
import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * DateDate 사이트 크롬(헤더·푸터) 단일화 가드 (ADR datedate/frontend/0004).
 *
 * <p>헤더·푸터가 두 벌(header/header-minimal, footer/footer-minimal)이던 동안 페이지마다 도입 시점에 따라
 * 다른 벌을 썼고, 그 편차가 고정 헤더 밑 브레드크럼 겹침·실제로 고정되지 않는 sticky·푸터 링크 누락으로 이어졌다.
 * 이 테스트는 모든 DateDate 페이지가 같은 헤더·같은 푸터를 쓰고, 투명 오버레이 헤더는 홈 히어로에만 쓰이는지 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteChromeConsistencyTest {

    /** 콘텐츠·허브·서비스 페이지를 섞은 대표 집합 — 옛 두 벌 양쪽 사용처를 모두 포함한다. */
    private static final List<String> PAGES = List.of(
            "/", "/about", "/faq", "/guide", "/guides", "/guides/how-to-pick-a-date",
            "/use-cases", "/use-cases/friend-meetup", "/tools/date-diff", "/insights/trends",
            "/login", "/privacy", "/terms");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("모든 DateDate 페이지가 단일 헤더를 쓰고, 투명 오버레이는 홈에만 쓰인다")
    void everyPageUsesSingleHeader_overlayOnlyOnHome() throws Exception {
        for (String path : PAGES) {
            String html = render(path);

            assertThat(count(html, "class=\"site-header\"")).as("%s site-header", path).isEqualTo(1);
            assertThat(html).as("%s 옛 링크 클래스", path).doesNotContain("nav-link-animated");
            if (path.equals("/")) {
                assertThat(html).as("홈 오버레이").contains("id=\"navbarMinimal\"");
            } else {
                assertThat(html).as("%s 는 오버레이가 아니다", path)
                        .doesNotContain("navbarMinimal")
                        .contains("class=\"navbar\"");
            }
        }
    }

    @Test
    @DisplayName("모든 DateDate 페이지가 같은 푸터를 쓰고, 연락처·정책·전 콘텐츠 링크를 한 번씩 담는다")
    void everyPageUsesSingleFooter() throws Exception {
        for (String path : PAGES) {
            String html = render(path);

            assertThat(count(html, "class=\"site-footer\"")).as("%s site-footer", path).isEqualTo(1);
            assertThat(html).as("%s 옛 푸터", path).doesNotContain("footer-minimal").doesNotContain("footer-nav");

            // /about 본문에도 같은 mailto·정책 링크가 있어 페이지 전체가 아닌 푸터 구간 안에서 센다.
            String footer = footerOf(html);
            assertThat(count(footer, "mailto:")).as("%s 푸터 mailto", path).isEqualTo(1);
            assertThat(count(footer, "href=\"/privacy\"")).as("%s 푸터 개인정보", path).isEqualTo(1);
            assertThat(count(footer, "href=\"/terms\"")).as("%s 푸터 약관", path).isEqualTo(1);
            assertThat(count(footer, "href=\"/use-cases/")).as("%s 푸터 활용 사례", path)
                    .isEqualTo(UseCaseSlugs.ALL.size());
            assertThat(count(footer, "href=\"/guides/")).as("%s 푸터 기사", path)
                    .isEqualTo(GuideSlugs.slugs().size());
        }
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String footerOf(String html) {
        int start = html.indexOf("<footer class=\"site-footer\"");
        assertThat(start).as("site-footer 시작").isGreaterThanOrEqualTo(0);
        int end = html.indexOf("</footer>", start);
        return html.substring(start, end);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
