package me.singingsandhill.calendar.datedate.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import jakarta.validation.Constraint;

/**
 * 요청 DTO 의 Bean Validation 문구는 메시지 키({validation.*})여야 하고 ko·en 번들에 모두 있어야 한다.
 * 키는 Spring Boot 의 MessageSourceMessageInterpolator 가 요청 로케일(웹 쿠키 lang·미니앱 Accept-Language)로 해석한다 —
 * 문구를 하드코딩하면 en 화면·미니앱에 한국어가 나가고, 키 오타는 응답에 "{validation...}" 이 그대로 노출된다.
 */
class ValidationMessageKeysTest {

    private static final String PACKAGE = "me.singingsandhill.calendar.datedate.presentation.dto.request";

    @Test
    @DisplayName("요청 DTO 검증 문구는 모두 ko·en 번들에 있는 메시지 키다")
    void constraintMessagesAreBundleKeys() throws Exception {
        Properties ko = load("messages.properties");
        Properties en = load("messages_en.properties");
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Class<?> dto : requestRecords()) {
            for (RecordComponent component : dto.getRecordComponents()) {
                for (Annotation annotation : component.getAccessor().getAnnotations()) {
                    if (!annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                        continue;
                    }
                    Method messageAttribute = annotation.annotationType().getMethod("message");
                    String message = (String) messageAttribute.invoke(annotation);
                    String where = dto.getSimpleName() + "." + component.getName()
                            + " @" + annotation.annotationType().getSimpleName();
                    checked++;
                    if (!message.startsWith("{validation.") || !message.endsWith("}")) {
                        problems.add(where + " — 메시지 키가 아님: " + message);
                        continue;
                    }
                    String key = message.substring(1, message.length() - 1);
                    if (!ko.containsKey(key)) {
                        problems.add(where + " — ko 번들에 없음: " + key);
                    }
                    if (!en.containsKey(key)) {
                        problems.add(where + " — en 번들에 없음: " + key);
                    }
                }
            }
        }

        assertThat(checked).as("검사한 검증 어노테이션 수").isGreaterThanOrEqualTo(32);
        assertThat(problems).isEmpty();
    }

    private static List<Class<?>> requestRecords() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((reader, factory) -> true);
        List<Class<?>> records = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(PACKAGE)) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (type.isRecord()) {
                records.add(type);
            }
        }
        return records;
    }

    private static Properties load(String name) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                ValidationMessageKeysTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
