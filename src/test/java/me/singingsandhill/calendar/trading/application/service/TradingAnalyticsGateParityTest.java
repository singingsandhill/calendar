package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.DivergenceResult;
import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;
import me.singingsandhill.calendar.trading.domain.signal.SignalSample;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 임계 반사실 분석의 매수 게이트가 실제 {@code SignalService.determineSignalType} 과 일치하는지 고정.
 *
 * <p>{@code TradingAnalyticsService.evaluateBuyGate} 는 신호 생성 로직을 <b>의도적으로 복제</b>한
 * 것이다. 공용 헬퍼로 추출하면 분석 기능을 위해 실주문 신호 경로를 수정해야 하는데, 그건 이 작업이
 * 통제하려는 바로 그 위험이다. 대신 이 테스트가 두 구현의 드리프트를 빌드 실패로 만든다.
 *
 * <p>여기가 깨지면 분석 페이지의 "임계를 50으로 올렸다면 몇 건이 통과했겠는가" 가 조용히 거짓말을
 * 하고 있다는 뜻이므로, 리팩터링 중이라도 무시하고 넘어가면 안 된다.
 */
class TradingAnalyticsGateParityTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 6, 10, 0);

    private final TradingProperties properties = new TradingProperties();
    private final SignalService signalService = new SignalService(null, null, null, properties);
    private final TradingAnalyticsService analytics =
            new TradingAnalyticsService(null, null, null, properties);

    private static BigDecimal bd(String v) {
        return v == null ? null : new BigDecimal(v);
    }

    /**
     * 하나의 시나리오를 두 세계에 각각 넣기 위한 입력 묶음.
     * 8개 구성점수의 합이 곧 totalScore 여야 두 구현이 같은 것을 본다.
     */
    private record Case(String name,
                        int maCross, int maTrend, int rsiDiv, int rsiLevel,
                        int stochDiv, int stochLevel, int volDiv, int rsiTrend,
                        String price, String ma60, String rsi, String stochK,
                        DivergenceType rsiDivergence,
                        String volumeMa, String currentVolume) {

        int totalScore() {
            return maCross + maTrend + rsiDiv + rsiLevel + stochDiv + stochLevel + volDiv + rsiTrend;
        }
    }

    private SignalType actualSignalType(Case c) throws Exception {
        IndicatorResult indicators = new IndicatorResult(
                bd(c.price()), null, null, bd(c.ma60()), bd(c.rsi()), bd(c.stochK()), null,
                bd(c.volumeMa()), bd(c.currentVolume()), 0, null);
        DivergenceResult divergence = new DivergenceResult(
                c.rsiDivergence(), DivergenceType.NONE, DivergenceType.NONE);

        Method m = SignalService.class.getDeclaredMethod("determineSignalType",
                int.class, DivergenceResult.class, IndicatorResult.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
        m.setAccessible(true);
        return (SignalType) m.invoke(signalService, c.totalScore(), divergence, indicators,
                c.maCross(), c.maTrend(), c.rsiDiv(), c.rsiLevel(),
                c.stochDiv(), c.stochLevel(), c.volDiv(), c.rsiTrend());
    }

    private SignalSample sampleOf(Case c) {
        return new SignalSample(1L, T0, SignalType.HOLD, c.totalScore(),
                c.maCross(), c.maTrend(), c.rsiDiv(), c.rsiLevel(),
                c.stochDiv(), c.stochLevel(), c.volDiv(), c.rsiTrend(),
                bd(c.ma60()), bd(c.rsi()), bd(c.stochK()), null,
                c.rsiDivergence(), DivergenceType.NONE, DivergenceType.NONE,
                bd(c.price()), bd(c.volumeMa()), bd(c.currentVolume()), null);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // 통과: 점수 충분(45), 동의 4개, MA60 상회
                new Case("aboveMa60_clearBuy", 25, 8, 0, 15, 0, 0, 0, 10,
                        "1100", "1000", "40", "50", DivergenceType.NONE, null, null),
                // 점수 미달 (35 < 40)
                new Case("scoreBelowThreshold", 25, 0, 0, 0, 0, 0, 0, 10,
                        "1100", "1000", "40", "50", DivergenceType.NONE, null, null),
                // 동의 지표 2개 (< 3)
                new Case("insufficientAgreement", 25, 0, 0, 20, 0, 0, 0, 0,
                        "1100", "1000", "40", "50", DivergenceType.NONE, null, null),
                // RSI 상한(70) 초과
                new Case("rsiTooHigh", 25, 8, 0, 15, 0, 0, 0, 10,
                        "1100", "1000", "75", "50", DivergenceType.NONE, null, null),
                // StochK 상한(85) 초과
                new Case("stochKTooHigh", 25, 8, 0, 15, 0, 0, 0, 10,
                        "1100", "1000", "40", "90", DivergenceType.NONE, null, null),
                // MA60 하회 + 확인 근거 없음 → HOLD
                new Case("belowMa60_noConfirmation", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", "1000", "40", "50", DivergenceType.NONE, "100", "100"),
                // MA60 하회 + 강세 다이버전스 → 통과
                new Case("belowMa60_bullishDivergence", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", "1000", "40", "50", DivergenceType.BULLISH, "100", "100"),
                // MA60 하회 + 강한 과매도(RSI < 30) → 통과
                new Case("belowMa60_strongOversold", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", "1000", "25", "50", DivergenceType.NONE, "100", "100"),
                // MA60 하회 + 거래량 스파이크(> volumeMa × 1.5) → 통과
                new Case("belowMa60_volumeSpike", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", "1000", "40", "50", DivergenceType.NONE, "100", "200"),
                // MA60 하회 + 거래량이 스파이크에 못 미침 → HOLD
                new Case("belowMa60_volumeBelowSpike", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", "1000", "40", "50", DivergenceType.NONE, "100", "120"),
                // ma60 자체가 없으면 확인 게이트 자체를 건너뛴다
                new Case("ma60Missing_skipsConfirmation", 25, 8, 0, 15, 0, 0, 0, 10,
                        "900", null, "40", "50", DivergenceType.NONE, null, null),
                // RSI 결측 → 매수 분기 실패
                new Case("rsiMissing", 25, 8, 0, 15, 0, 0, 0, 10,
                        "1100", "1000", null, "50", DivergenceType.NONE, null, null),
                // StochK 결측 → 매수 분기 실패
                new Case("stochKMissing", 25, 8, 0, 15, 0, 0, 0, 10,
                        "1100", "1000", "40", null, DivergenceType.NONE, null, null),
                // 경계: 점수 정확히 40
                new Case("scoreExactlyAtThreshold", 25, 0, 0, 15, 0, 0, 0, 0,
                        "1100", "1000", "40", "50", DivergenceType.NONE, null, null)
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void evaluateBuyGate_matchesSignalServiceDetermineSignalType(Case c) throws Exception {
        boolean actualIsBuy = actualSignalType(c) == SignalType.BUY;
        boolean gatePasses = analytics.evaluateBuyGate(sampleOf(c), properties.getThresholds().getSignalBuy())
                .passesLow();

        assertThat(gatePasses)
                .as("case=%s score=%d — 분석 게이트와 SignalService 판정이 갈렸다", c.name(), c.totalScore())
                .isEqualTo(actualIsBuy);
    }

    @Test
    void indeterminateWhenVolumeContextMissingBelowMa60() {
        // volumeMa/currentVolume 이 없는 과거 행 — 거래량 스파이크 분기를 판정할 수 없다.
        Case c = new Case("legacyRow", 25, 8, 0, 15, 0, 0, 0, 10,
                "900", "1000", "40", "50", DivergenceType.NONE, null, null);

        TradingAnalyticsService.GateOutcome outcome =
                analytics.evaluateBuyGate(sampleOf(c), 40);

        // 거짓 확신 대신 범위를 낸다. low(스파이크 없음)=차단, high(스파이크 있음)=통과.
        assertThat(outcome.indeterminate()).isTrue();
        assertThat(outcome.passesLow()).isFalse();
        assertThat(outcome.passesHigh()).isTrue();
    }

    @Test
    void countAgreeing_matchesSignalServiceCounting() throws Exception {
        Case c = new Case("mixed", 25, -8, 0, 15, 0, 0, -20, 10,
                "1100", "1000", "40", "50", DivergenceType.NONE, null, null);

        Method m = SignalService.class.getDeclaredMethod("countAgreeingIndicators", int.class, int[].class);
        m.setAccessible(true);
        int expected = (int) m.invoke(signalService, c.totalScore(),
                new int[]{c.maCross(), c.maTrend(), c.rsiDiv(), c.rsiLevel(),
                        c.stochDiv(), c.stochLevel(), c.volDiv(), c.rsiTrend()});

        assertThat(TradingAnalyticsService.countAgreeing(sampleOf(c))).isEqualTo(expected);
    }
}
