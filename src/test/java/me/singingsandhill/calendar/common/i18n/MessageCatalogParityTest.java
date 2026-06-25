package me.singingsandhill.calendar.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ko/en 메시지 카탈로그 정합성 가드.
 *
 * <p>한쪽 카탈로그에만 키를 추가하면 영어 페이지에 한국어가 노출되거나(또는 그 반대)
 * use-case 섹션이 조용히 숨겨진다 (detail.html 의 msgOrNull 게이트). AdSense 리뷰
 * 관점에서 "광고된 영문 페이지가 한국어로 응답" 하는 정합성 사고를 빌드 단계에서 차단한다.
 */
class MessageCatalogParityTest {

    // 카탈로그는 유니코드 escape(backslash-u)와 raw UTF-8 한국어가 혼재 → 반드시 UTF-8 Reader 로 로드
    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(MessageCatalogParityTest.class.getResourceAsStream("/" + name),
                        name + " not found on classpath"),
                StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    @Test
    @DisplayName("messages.properties 와 messages_en.properties 의 키 집합이 완전히 일치한다")
    void keySetsAreIdentical() throws IOException {
        Properties ko = load("messages.properties");
        Properties en = load("messages_en.properties");

        assertThat(en.stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(ko.stringPropertyNames());
    }

    @Test
    @DisplayName("양쪽 카탈로그에 빈 값이 없다")
    void noBlankValues() throws IOException {
        for (String file : List.of("messages.properties", "messages_en.properties")) {
            Properties p = load(file);
            for (String key : p.stringPropertyNames()) {
                assertThat(p.getProperty(key).strip())
                        .as(file + " :: " + key)
                        .isNotEmpty();
            }
        }
    }
}
