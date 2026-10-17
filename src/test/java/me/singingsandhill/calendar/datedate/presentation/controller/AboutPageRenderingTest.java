package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

/**
 * About 신뢰 정보 가드 (AdSense ② — ADR common/seo/0015).
 * 사람 리뷰어가 보는 운영자 신원(GitHub 링크)과, 사용자 결정으로 없앤 "광고로 운영비 충당" 문구의 재유입을 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AboutPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "/about [{0}]")
    @ValueSource(strings = {"ko", "en"})
    @DisplayName("About 은 운영자 GitHub 링크를 새 탭(rel=me)으로 화면에 보여준다")
    void showsOperatorGithubLink(String lang) throws Exception {
        String html = render("/about", lang);
        assertThat(html).containsPattern(
                "href=\"https://github.com/singingsandhill\"[^>]*target=\"_blank\"[^>]*rel=\"me noopener noreferrer\"");
    }

    @Test
    @DisplayName("About(ko) 에 만든 사람 섹션이 있다")
    void koreanMakerSection() throws Exception {
        String text = visibleText(render("/about", "ko"));
        assertThat(text).contains("만든 사람").contains("1인 개발자가 만들고 운영하는 개인 프로젝트");
    }

    @Test
    @DisplayName("About(en) 에 만든 사람 섹션이 있다")
    void englishMakerSection() throws Exception {
        String text = visibleText(render("/about", "en"));
        assertThat(text).contains("Who's behind DateDate").contains("built and run by a single developer");
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @CsvSource({"/about,ko", "/about,en", "/terms,ko", "/terms,en"})
    @DisplayName("광고로 운영비를 충당한다는 문구가 본문·메타 어디에도 없다 (사용자 결정 2026-10-14)")
    void noAdFundingCopy(String path, String lang) throws Exception {
        String html = HtmlUtils.htmlUnescape(render(path, lang));
        assertThat(html)
                .doesNotContain("광고로 충당")
                .doesNotContain("광고 운영 기준")
                .doesNotContain("서비스 운영을 위해")
                .doesNotContain("hosting costs with ads")
                .doesNotContain("To keep the service running");
    }

    private String render(String path, String lang) throws Exception {
        return mockMvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")).replaceAll("\\s+", " ");
    }
}
