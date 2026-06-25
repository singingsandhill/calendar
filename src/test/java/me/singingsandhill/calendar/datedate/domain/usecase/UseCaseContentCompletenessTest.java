package me.singingsandhill.calendar.datedate.domain.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Use-case 콘텐츠 완결성 가드.
 *
 * <p>use-cases/detail.html 은 {@code #messages.msgOrNull} 게이트로 키가 없는 섹션을
 * 조용히 숨긴다 — 번역 누락 시 에러 없이 페이지가 thin 해진다. 모든 슬러그가 5개 섹션
 * (intro / when-to-use / mistakes / tips / FAQ) 의 전체 키 패밀리를 양 로케일에
 * 가지는지 검증해 "절반만 렌더되는 use-case 페이지" 를 빌드에서 차단한다.
 */
class UseCaseContentCompletenessTest {

    private static final List<String> REQUIRED = buildRequiredSuffixes();

    private static List<String> buildRequiredSuffixes() {
        List<String> suffixes = new ArrayList<>();
        // head 키 — SeoService.getUseCaseSeo / buildUseCaseHowToJsonLd 가 JSON-LD 에 사용
        suffixes.add("title");
        suffixes.add("description");
        suffixes.add("body");
        suffixes.add("exampleLabel");
        suffixes.add("exampleId");
        for (int i = 1; i <= 3; i++) {
            suffixes.add("step" + i + ".name");
            suffixes.add("step" + i + ".text");
        }
        // 데이터 구동 푸터/네비 라벨 (footer.html th:each → seo.useCase.<slug>.navLabel)
        suffixes.add("navLabel");
        // 슬러그별 워크드 예시 (detail.html sample 블록)
        suffixes.add("sample.title");
        suffixes.add("sample.body");
        // 템플릿 게이트 키 (msgOrNull 분기 지점)
        suffixes.add("section.intro");
        suffixes.add("section.whenToUse.lead");
        suffixes.add("section.mistakes.lead");
        suffixes.add("section.tips.title");
        suffixes.add("section.faq.q1");
        // 각 섹션의 전체 패밀리
        suffixes.add("section.whenToUse.title");
        for (int i = 1; i <= 3; i++) {
            suffixes.add("section.whenToUse.scenario" + i + ".title");
            suffixes.add("section.whenToUse.scenario" + i + ".text");
        }
        suffixes.add("section.mistakes.title");
        for (int i = 1; i <= 3; i++) {
            suffixes.add("section.mistakes.mistake" + i + ".problem");
            suffixes.add("section.mistakes.mistake" + i + ".fix");
        }
        for (int i = 1; i <= 4; i++) {
            suffixes.add("section.tips.tip" + i + ".title");
            suffixes.add("section.tips.tip" + i + ".text");
        }
        for (int i = 1; i <= 5; i++) {
            suffixes.add("section.faq.q" + i);
            suffixes.add("section.faq.a" + i);
        }
        return suffixes;
    }

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(UseCaseContentCompletenessTest.class.getResourceAsStream("/" + name),
                        name + " not found on classpath"),
                StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    @Test
    @DisplayName("모든 use-case 슬러그는 양 로케일에 5개 섹션 키 패밀리를 전부 가진다")
    void everySlugHasAllSectionsInBothLocales() throws IOException {
        Properties ko = load("messages.properties");
        Properties en = load("messages_en.properties");

        for (String slug : UseCaseSlugs.ALL) {
            for (String suffix : REQUIRED) {
                String key = "seo.useCase." + slug + "." + suffix;
                assertThat(ko.getProperty(key)).as("ko missing: " + key).isNotBlank();
                assertThat(en.getProperty(key)).as("en missing: " + key).isNotBlank();
            }
        }
    }
}
