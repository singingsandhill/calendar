package me.singingsandhill.calendar.datedate.presentation.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import me.singingsandhill.calendar.datedate.presentation.dto.response.TextSegment;

/**
 * 일정 설명의 링크 판별 — 웹(Thymeleaf)이 th:href 로 그리는 유일한 근거라 http(s) 외 스킴이 링크가 되면 안 된다
 * (ADR datedate/domain/0011).
 */
class DescriptionLinksTest {

    private static TextSegment text(String value) {
        return new TextSegment(value, null);
    }

    private static TextSegment link(String url) {
        return new TextSegment(url, url);
    }

    @Test
    @DisplayName("null·빈 문자열은 조각이 없다")
    void emptyInput_noSegments() {
        assertThat(DescriptionLinks.split(null)).isEmpty();
        assertThat(DescriptionLinks.split("")).isEmpty();
    }

    @Test
    @DisplayName("URL 이 없으면 평문 조각 하나")
    void plainText_singleTextSegment() {
        assertThat(DescriptionLinks.split("회의 60분\n장소 미정")).containsExactly(text("회의 60분\n장소 미정"));
    }

    @Test
    @DisplayName("문장 중간의 https URL 을 링크로 나눈다")
    void urlInSentence_split() {
        assertThat(DescriptionLinks.split("안건: https://notion.so/agenda 확인"))
                .containsExactly(text("안건: "), link("https://notion.so/agenda"), text(" 확인"));
    }

    @Test
    @DisplayName("한글 조사가 URL 에 바로 붙어도 조사 앞에서 끊는다")
    void koreanParticleAttached_stopsBeforeHangul() {
        assertThat(DescriptionLinks.split("https://notion.so/a에서 확인"))
                .containsExactly(link("https://notion.so/a"), text("에서 확인"));
    }

    @Test
    @DisplayName("끝의 문장부호와 짝 없는 닫는 괄호는 링크에서 뺀다")
    void trailingPunctuationAndUnbalancedParen_excluded() {
        assertThat(DescriptionLinks.split("참고 https://x.com/a."))
                .containsExactly(text("참고 "), link("https://x.com/a"), text("."));
        assertThat(DescriptionLinks.split("(https://x.com/a)"))
                .containsExactly(text("("), link("https://x.com/a"), text(")"));
    }

    @Test
    @DisplayName("URL 안의 짝 맞는 괄호와 쿼리 문자열은 유지한다")
    void balancedParenAndQuery_kept() {
        assertThat(DescriptionLinks.split("https://en.wikipedia.org/wiki/Foo_(bar)"))
                .containsExactly(link("https://en.wikipedia.org/wiki/Foo_(bar)"));
        assertThat(DescriptionLinks.split("https://b.com/x?y=1&z=2#top"))
                .containsExactly(link("https://b.com/x?y=1&z=2#top"));
    }

    @Test
    @DisplayName("여러 링크와 줄바꿈이 섞여도 순서대로 나눈다")
    void multipleLinksAcrossLines() {
        assertThat(DescriptionLinks.split("자료 https://a.com\n단톡 http://b.com/c"))
                .containsExactly(text("자료 "), link("https://a.com"), text("\n단톡 "), link("http://b.com/c"));
    }

    @Test
    @DisplayName("스킴은 대소문자를 가리지 않는다")
    void schemeCaseInsensitive() {
        assertThat(DescriptionLinks.split("HTTPS://Example.com/A"))
                .containsExactly(link("HTTPS://Example.com/A"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "www.example.com",
            "javascript:alert(1)",
            "ftp://example.com/file",
            "mailto:a@b.com",
            "https:// 주소 없음",
            "data:text/html,<script>alert(1)</script>"
    })
    @DisplayName("http(s):// 뒤에 주소가 있는 경우만 링크다")
    void nonHttpOrEmptyHost_notLinked(String input) {
        assertThat(DescriptionLinks.split(input)).containsExactly(text(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "안건: https://notion.so/agenda 확인",
            "(https://x.com/a). 끝",
            "https://a.com/에서\nhttp://b.com, https://c.com!",
            "https://."
    })
    @DisplayName("조각의 글자를 이으면 원문과 같다 — 잃거나 더하는 글자가 없다")
    void segmentsConcatenateToInput(String input) {
        List<TextSegment> segments = DescriptionLinks.split(input);

        assertThat(segments.stream().map(TextSegment::text).collect(Collectors.joining())).isEqualTo(input);
    }
}
