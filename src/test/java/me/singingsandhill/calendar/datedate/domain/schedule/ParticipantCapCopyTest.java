package me.singingsandhill.calendar.datedate.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 한도를 숫자로 말하는 문구가 Schedule.MAX_PARTICIPANTS 와 같은지 — 한도를 바꾸면 이 키들도 같이 바꿔야 한다.
 * 8→10 변경 때 홈·가이드·FAQ·소개·use-case 문구가 "8명" 으로 남을 뻔했다 (ADR datedate/domain/0010).
 * 일반 조언 속 숫자("6~8명 모임" 등)는 한도와 무관해 목록에 없다.
 */
class ParticipantCapCopyTest {

    private static final int MAX = Schedule.MAX_PARTICIPANTS;

    private static final List<String> CAP_KEYS = List.of(
            "index.feature2.desc",
            "index.feature3.title",
            "index.why.limits.item2",
            "guide.step4.li1",
            "guide.troubleshooting.q3",
            "guide.troubleshooting.a3",
            "faq.a2",
            "about.section.audience.body",
            "seo.useCase.team-meeting.body",
            "seo.useCase.friend-meetup.section.faq.q1",
            "seo.useCase.friend-meetup.section.faq.a1",
            "seo.useCase.travel-planning.section.faq.q1",
            "seo.useCase.travel-planning.section.faq.a1",
            "seo.useCase.club-activity.section.faq.a1",
            "seo.useCase.club-activity.section.faq.a3");

    // 영문 q3 는 "Our team is 11 people" 로 한도를 말하지 않는다
    private static final String KO_ONLY_CAP_KEY = "seo.useCase.team-meeting.section.faq.q3";

    // 카탈로그는 유니코드 escape 와 raw UTF-8 이 혼재 → UTF-8 Reader 로 로드 (MessageCatalogParityTest 와 같은 방식)
    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(ParticipantCapCopyTest.class.getResourceAsStream("/" + name),
                        name + " not found on classpath"),
                StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    @Test
    @DisplayName("ko 문구가 현재 한도(N명)를 말한다")
    void koreanCopyStatesCurrentCap() throws IOException {
        Properties ko = load("messages.properties");
        Pattern cap = Pattern.compile("(?<!\\d)" + MAX + "명");
        List<String> keys = new ArrayList<>(CAP_KEYS);
        keys.add(KO_ONLY_CAP_KEY);

        for (String key : keys) {
            assertThat(cap.matcher(ko.getProperty(key)).find()).as(key).isTrue();
        }
    }

    @Test
    @DisplayName("en 문구가 현재 한도(숫자 N)를 말한다")
    void englishCopyStatesCurrentCap() throws IOException {
        Properties en = load("messages_en.properties");
        Pattern cap = Pattern.compile("(?<!\\d)" + MAX + "(?!\\d)");

        for (String key : CAP_KEYS) {
            assertThat(cap.matcher(en.getProperty(key)).find()).as(key).isTrue();
        }
    }

    @Test
    @DisplayName("한도는 서버가 막는 하드 한도라 '권장'/'recommend' 로 부르지 않는다 — 11번째는 409")
    void capCopyDoesNotCallTheHardCapARecommendation() throws IOException {
        Properties ko = load("messages.properties");
        Properties en = load("messages_en.properties");
        List<String> koKeys = new ArrayList<>(CAP_KEYS);
        koKeys.add(KO_ONLY_CAP_KEY);

        for (String key : koKeys) {
            assertThat(ko.getProperty(key)).as("ko " + key).doesNotContain("권장");
        }
        for (String key : CAP_KEYS) {
            assertThat(en.getProperty(key)).as("en " + key).doesNotContainIgnoringCase("recommend");
        }
    }

    @Test
    @DisplayName("참여자 카운터는 한도를 숫자로 적지 않고 인자 {1} 로 받는다")
    void participantCounterTakesCapAsArgument() throws IOException {
        for (String file : List.of("messages.properties", "messages_en.properties")) {
            String counter = load(file).getProperty("schedule.participant.count");

            assertThat(counter).as(file).contains("{1}");
            assertThat(counter.replaceAll("\\{\\d}", "")).as(file).doesNotContainPattern("\\d");
        }
    }
}
