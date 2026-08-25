package me.singingsandhill.calendar.datedate.domain.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OwnerIdGeneratorTest {

    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^[a-z0-9-]{2,20}$");

    @Test
    @DisplayName("같은 시드는 같은 ID 를 낸다 (테스트 결정성)")
    void sameSeedProducesSameId() {
        String first = new OwnerIdGenerator(new Random(20260813L)).next();
        String second = new OwnerIdGenerator(new Random(20260813L)).next();

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("생성한 ID 는 owner 라우트·도메인 제약을 모두 통과한다")
    void generatedIdsSatisfyOwnerConstraints() {
        OwnerIdGenerator generator = new OwnerIdGenerator(new Random(7L));

        for (int i = 0; i < 500; i++) {
            String id = generator.next();

            assertThat(id).matches(OWNER_ID_PATTERN);
            assertThat(ReservedOwnerIds.isReserved(id)).isFalse();
            assertThatCode(() -> new Owner(id)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("단어 목록은 소문자 2~7자, 중복 없음 — 최장 조합도 20자를 넘지 않는다")
    void wordListsFitTheTwentyCharacterBudget() {
        Set<String> all = new HashSet<>();
        all.addAll(OwnerIdGenerator.ADJECTIVES);
        all.addAll(OwnerIdGenerator.NOUNS);

        assertThat(OwnerIdGenerator.ADJECTIVES).allMatch(w -> w.matches("^[a-z]{2,7}$"));
        assertThat(OwnerIdGenerator.NOUNS).allMatch(w -> w.matches("^[a-z]{2,7}$"));
        assertThat(all).hasSize(OwnerIdGenerator.ADJECTIVES.size() + OwnerIdGenerator.NOUNS.size());

        int longest = OwnerIdGenerator.ADJECTIVES.stream().mapToInt(String::length).max().orElseThrow()
                + OwnerIdGenerator.NOUNS.stream().mapToInt(String::length).max().orElseThrow()
                + "--".length() + "9999".length();
        assertThat(longest).isLessThanOrEqualTo(20);
    }

    @Test
    @DisplayName("조합 수는 14,400,000 — 기존 14,400 의 1000배")
    void combinationsAreOneThousandTimesTheOldSpace() {
        assertThat(OwnerIdGenerator.combinations()).isEqualTo(14_400_000);
    }

    @Test
    @DisplayName("세 자리 구성 요소가 모두 흔들린다 (한 축이 고정되지 않는다)")
    void everyComponentVaries() {
        OwnerIdGenerator generator = new OwnerIdGenerator(new Random(99L));
        Set<String> adjectives = new HashSet<>();
        Set<String> nouns = new HashSet<>();
        Set<String> suffixes = new HashSet<>();

        for (int i = 0; i < 300; i++) {
            String[] parts = generator.next().split("-");
            assertThat(parts).hasSize(3);
            adjectives.add(parts[0]);
            nouns.add(parts[1]);
            suffixes.add(parts[2]);
        }

        assertThat(adjectives.size()).isGreaterThan(20);
        assertThat(nouns.size()).isGreaterThan(20);
        assertThat(suffixes.size()).isGreaterThan(250);
    }
}
