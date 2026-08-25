package me.singingsandhill.calendar.datedate.domain.owner;

import java.security.SecureRandom;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 홈 화면 "랜덤 생성" 버튼이 제안하는 owner ID 를 만든다 ({@code brave-otter-4821} 형태).
 *
 * <p>가짓수는 {@link #combinations()} = 14,400,000. 도메인 계층이라 저장소를 모르므로
 * 여기서는 <em>만들기만</em> 한다 — 미사용 여부 확인은 OwnerService 가 한다.
 *
 * <p>{@link RandomGenerator} 를 주입받는 이유는 stock 모듈이 {@code Clock} 빈을 주입해
 * 시간 의존 코드를 결정적으로 테스트하는 것과 같다. 기본값이 {@link SecureRandom} 인 것은
 * 예측 가능한 시퀀스면 다음 사용자에게 제안될 ID 를 미리 선점해 방해할 수 있기 때문.
 */
public class OwnerIdGenerator {

    /** 각 단어는 소문자 2~7자. 최장 조합이 20자(= owner ID 상한)를 넘지 않도록 유지할 것. */
    static final List<String> ADJECTIVES = List.of(
            "happy", "cool", "fast", "lucky", "sunny", "cozy", "brave", "calm",
            "kind", "smart", "sweet", "wild", "bright", "quiet", "gentle", "merry",
            "swift", "clever", "jolly", "neat", "bold", "fresh", "warm", "clear",
            "keen", "lively", "tidy", "humble", "eager", "mellow", "nimble", "plucky",
            "snug", "steady", "cheery", "quirky", "rustic", "silky", "breezy", "dandy");

    static final List<String> NOUNS = List.of(
            "cat", "dog", "star", "moon", "sun", "tree", "bird", "fish",
            "bear", "wolf", "lion", "fox", "otter", "pine", "heron", "cloud",
            "river", "stone", "maple", "robin", "panda", "koala", "tiger", "whale",
            "seal", "crane", "finch", "willow", "cedar", "ember", "comet", "meadow",
            "harbor", "island", "forest", "garden", "lantern", "acorn", "sparrow", "badger");

    private static final int SUFFIX_MIN = 1000;
    private static final int SUFFIX_COUNT = 9000;

    private final RandomGenerator random;

    public OwnerIdGenerator() {
        this(new SecureRandom());
    }

    public OwnerIdGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String next() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int suffix = SUFFIX_MIN + random.nextInt(SUFFIX_COUNT);
        return adjective + "-" + noun + "-" + suffix;
    }

    public static int combinations() {
        return ADJECTIVES.size() * NOUNS.size() * SUFFIX_COUNT;
    }
}
