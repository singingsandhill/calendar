package me.singingsandhill.calendar.common.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 템플릿/메시지 카탈로그 placeholder 위생 가드.
 *
 * <p>AdSense 정책은 under-construction / placeholder 화면 노출을 저가치 신호로 본다.
 * "TODO", "lorem", "coming soon" 류 마커가 공개 템플릿이나 카탈로그에 들어가는 순간
 * 빌드에서 실패시킨다.
 */
class TemplatePlaceholderHygieneTest {

    // "TODO" 는 대소문자 구분 + 단어 경계로만 매치 (css class 등 오탐 방지);
    // 나머지 마커는 소문자화 후 부분 문자열 매치.
    private static final Pattern TODO = Pattern.compile("\\bTODO\\b");
    private static final List<String> MARKERS = List.of("lorem", "coming soon", "준비 중", "공사 중");

    @Test
    @DisplayName("템플릿/메시지 카탈로그에 placeholder 마커가 없다")
    void noPlaceholderMarkers() throws IOException {
        Path base = Path.of("src/main/resources");   // Gradle 테스트 작업 디렉토리 == 프로젝트 루트
        List<Path> targets = new ArrayList<>(List.of(
                base.resolve("messages.properties"),
                base.resolve("messages_en.properties")));
        try (Stream<Path> walk = Files.walk(base.resolve("templates"))) {
            walk.filter(p -> p.toString().endsWith(".html")).forEach(targets::add);
        }

        for (Path file : targets) {
            String text = Files.readString(file);
            String lower = text.toLowerCase(Locale.ROOT);
            for (String marker : MARKERS) {
                assertThat(lower).as(file + " contains '" + marker + "'").doesNotContain(marker);
            }
            assertThat(TODO.matcher(text).find()).as(file + " contains TODO").isFalse();
        }
    }
}
