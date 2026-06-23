package me.singingsandhill.calendar.stock.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기동 시 설정 검증기의 경고 규칙 회귀 테스트.
 * 진단 전용이므로 동작 변경 없음 — collectWarnings() 의 순수 판정만 검증.
 */
class StockBotConfigValidatorTest {

    private StockProperties enabledProps() {
        StockProperties p = new StockProperties();
        p.getBot().setEnabled(true);
        p.getBot().setMode(StockProperties.Bot.Mode.PAPER);   // 기본 LIVE 경고를 끄고 시작
        p.getUniverse().setRankApiTop(30);
        p.getUniverse().setFallbackCodes(List.of("005930"));
        return p;
    }

    private StockBotConfigValidator validator(StockProperties p) {
        return new StockBotConfigValidator(p);
    }

    @Test
    @DisplayName("봇 비활성 시 경고 없음")
    void noWarningsWhenDisabled() {
        StockProperties p = enabledProps();
        p.getBot().setEnabled(false);
        assertThat(validator(p).collectWarnings()).isEmpty();
    }

    @Test
    @DisplayName("LIVE 모드 + 활성 → 실주문 경고")
    void warnsOnLiveMode() {
        StockProperties p = enabledProps();
        p.getBot().setMode(StockProperties.Bot.Mode.LIVE);
        assertThat(validator(p).collectWarnings())
            .anyMatch(w -> w.contains("LIVE"));
    }

    @Test
    @DisplayName("정상 PAPER 설정 → 경고 없음")
    void noWarningsForHealthyPaperConfig() {
        assertThat(validator(enabledProps()).collectWarnings()).isEmpty();
    }

    @Test
    @DisplayName("rank 비활성 + fallback/pinned 모두 비어있음 → 빈 유니버스 경고")
    void warnsOnEmptyUniverse() {
        StockProperties p = enabledProps();
        p.getUniverse().setRankApiTop(0);
        p.getUniverse().setFallbackCodes(List.of());
        p.getUniverse().setPinned(List.of());
        assertThat(validator(p).collectWarnings())
            .anyMatch(w -> w.contains("유니버스가 비어있음"));
    }

    @Test
    @DisplayName("rank 비활성(but fallback 존재) → 정적 풀 전용 경고")
    void warnsWhenRankDisabledButFallbackPresent() {
        StockProperties p = enabledProps();
        p.getUniverse().setRankApiTop(0);
        p.getUniverse().setFallbackCodes(List.of("005930"));
        p.getUniverse().setPinned(List.of());
        assertThat(validator(p).collectWarnings())
            .anyMatch(w -> w.contains("정적 fallback-codes 만 사용"));
    }

    @Test
    @DisplayName("floor-trade-strength > entry-min-strength → 부정합 경고")
    void warnsOnStrengthThresholdIncoherence() {
        StockProperties p = enabledProps();
        p.getScreening().setFloorTradeStrength(new BigDecimal("120"));
        p.getEntry().setEntryMinStrength(new BigDecimal("100"));
        assertThat(validator(p).collectWarnings())
            .anyMatch(w -> w.contains("스크리닝이 진입보다 엄격"));
    }

    @Test
    @DisplayName("현재 기본값(floor 95 < entry 100)은 부정합 경고를 내지 않는다")
    void noIncoherenceWarningForDefaultThresholds() {
        // enabledProps 는 StockProperties 기본 floor(95)/entry(100) 유지
        assertThat(validator(enabledProps()).collectWarnings())
            .noneMatch(w -> w.contains("스크리닝이 진입보다 엄격"));
    }

    @Test
    @DisplayName("mail.enabled=true 인데 to 미설정 → 메일 경고")
    void warnsOnMailEnabledWithoutRecipient() {
        StockProperties p = enabledProps();
        p.getMail().setEnabled(true);
        p.getMail().setTo("");
        assertThat(validator(p).collectWarnings())
            .anyMatch(w -> w.contains("mail.to 미설정"));
    }

    @Test
    @DisplayName("유효 설정 요약에 핵심 키가 포함된다")
    void summaryContainsKeyFields() {
        String summary = validator(enabledProps()).effectiveConfigSummary();
        assertThat(summary)
            .contains("mode=PAPER")
            .contains("거래량순위 top-30")
            .contains("entryMinStrength");
    }
}
