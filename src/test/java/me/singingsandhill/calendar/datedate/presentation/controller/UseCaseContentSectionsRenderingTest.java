package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * use-case 상세 본문 블록 렌더링 가드.
 *
 * <p>같은 요소에 {@code th:with} 와 {@code th:if} 를 두면 Thymeleaf 는 {@code th:if} 를 먼저 평가해 변수가
 * null 인 채로 섹션을 지운다. 2026-05-16(fe46757)부터 10-13 까지 라이브 use-case 5페이지가 이 이유로
 * 소개·예시·사용 시점·실수·팁·FAQ 를 하나도 보여주지 못했고, 기존 렌더 테스트는 제목 단어만 확인해 통과했다.
 * 그래서 여기서는 각 블록의 실제 문구가 화면 텍스트에 있는지 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UseCaseContentSectionsRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageSource messageSource;

    @Test
    @DisplayName("friend-meetup(ko) 화면에 소개 문단·진행 순서 첫 단계·FAQ 첫 질문이 나온다")
    void friendMeetupKoreanShowsBodySections() throws Exception {
        String text = visibleText(render("friend-meetup", "ko"));

        assertThat(text).contains("친구와 약속 잡는 게 왜 이렇게 어려울까요");
        assertThat(text).contains("DateDate 페이지 만들기");
        assertThat(text).contains("친구가 10명 넘으면 어떻게 하나요?");
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @MethodSource("slugsAndLanguages")
    @DisplayName("모든 슬러그·로케일에서 소개·진행 순서·예시·사용 시점·실수·팁·FAQ 블록이 화면에 나온다")
    void everyUseCaseShowsAllBodySections(String slug, String lang) throws Exception {
        Locale locale = Locale.forLanguageTag(lang);
        String text = visibleText(render(slug, lang));

        for (String key : List.of("section.intro", "step1.name", "sample.title", "section.whenToUse.lead",
                "section.mistakes.lead", "section.tips.title", "section.faq.q1")) {
            String expected = normalize(messageSource.getMessage("seo.useCase." + slug + "." + key, null, locale));
            assertThat(text).as("%s [%s] %s", slug, lang, key).contains(expected);
        }
    }

    static Stream<Arguments> slugsAndLanguages() {
        return UseCaseSlugs.ALL.stream()
                .flatMap(slug -> Stream.of(Arguments.of(slug, "ko"), Arguments.of(slug, "en")));
    }

    private String render(String slug, String lang) throws Exception {
        return mockMvc.perform(get("/use-cases/" + slug).param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return normalize(HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")));
    }

    private static String normalize(String s) {
        return s.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }
}
