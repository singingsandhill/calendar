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
import org.springframework.web.util.HtmlUtils;

/**
 * /guide 6단계 가드 (AdSense ② — 시간 투표 단계 신설, 일정 생성 안내 정정).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuidePageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/guide(ko) — 6단계, 5단계는 시간 정하기, 6단계는 결과 확인, 일정 생성 안내가 실제 흐름과 맞다")
    void koreanSixSteps() throws Exception {
        String html = render("ko");
        assertThat(html).contains("id=\"step-6\"").doesNotContain("id=\"step-7\"");
        String text = visibleText(html);
        assertThat(text).contains("6단계로").contains("시간 정하기").contains("결과 확인 & 결정")
                .contains("30분 단위").contains("이 일정 만들기").contains("일정 설명(최대 500자)")
                .doesNotContain("자동으로 생성됩니다").doesNotContain("겨치는");
    }

    @Test
    @DisplayName("/guide(en) — 6단계, Pick a Time, 실제 생성 흐름")
    void englishSixSteps() throws Exception {
        String text = visibleText(render("en"));
        assertThat(text).contains("in 6 steps").contains("Pick a Time").contains("Review Results & Decide")
                .contains("30-minute steps").contains("Create this schedule").contains("up to 500 characters")
                .doesNotContain("automatically created");
    }

    @Test
    @DisplayName("/tools/date-diff 의 /guide 소개도 6단계")
    void dateDiffDescribesSixStepGuide() throws Exception {
        assertThat(visibleText(render("/tools/date-diff", "ko"))).contains("6단계 가이드").doesNotContain("5단계 가이드");
        assertThat(visibleText(render("/tools/date-diff", "en"))).contains("six-step guide").doesNotContain("five-step");
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "/guide [{0}]")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ko", "en"})
    @DisplayName("단계마다 스크린샷이 있고 width·height·alt 를 갖추며, 버전 URL 로 실제 서빙된다")
    void everyStepHasServedScreenshot(String lang) throws Exception {
        String html = render(lang);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<img[^>]*class=\"guide-shot-img\"[^>]*>").matcher(html);
        int count = 0;
        while (m.find()) {
            String tag = m.group();
            assertThat(tag).containsPattern("width=\"\\d+\"").containsPattern("height=\"\\d+\"")
                    .containsPattern("alt=\"[^\"]+\"");
            java.util.regex.Matcher srcMatch = java.util.regex.Pattern.compile("src=\"([^\"]+)\"").matcher(tag);
            assertThat(srcMatch.find()).isTrue();
            String src = srcMatch.group(1);
            assertThat(src).contains("/image/guide/" + lang + "/step");
            mockMvc.perform(get(src))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().contentType("image/webp"));
            count++;
        }
        assertThat(count).isEqualTo(6);
        assertThat(visibleText(html)).contains("ko".equals(lang) ? "예시 화면" : "Example screen");
    }

    private String render(String lang) throws Exception {
        return render("/guide", lang);
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
