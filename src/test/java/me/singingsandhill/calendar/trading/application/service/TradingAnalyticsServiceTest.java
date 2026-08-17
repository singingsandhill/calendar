package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.AnalyticsReport;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;
import me.singingsandhill.calendar.trading.domain.signal.SignalSample;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 신호 품질 분석의 순수 집계 (ADR trading/observability/0001).
 *
 * <p>여기서 지키려는 것은 "숫자가 나온다" 가 아니라 <b>숫자가 오도하지 않는다</b> 이다. 이 리포트는
 * 실자금 파라미터를 바꿀 근거로 쓰이므로, 겹치는 표본을 독립인 양 보여주거나 인과 방향이 뒤집힌
 * 조인을 하거나 적자 장부에서 정의되지 않는 비율을 계산하면 잘못된 확신을 만든다.
 */
class TradingAnalyticsServiceTest {

    private static final String MARKET = "KRW-ADA";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 1, 9, 0);

    private final TradingProperties properties = new TradingProperties();
    private final TradingAnalyticsService service =
            new TradingAnalyticsService(null, null, null, properties);

    /** 점수와 가격만 다른 최소 관측치. 게이트를 통과하도록 지표를 중립값으로 채운다. */
    private SignalSample sample(int minuteOffset, int totalScore, String price) {
        return new SignalSample(
                (long) minuteOffset, T0.plusMinutes(minuteOffset), SignalType.HOLD, totalScore,
                totalScore, 0, 0, 0, 0, 0, 0, 0,
                new BigDecimal("900"), new BigDecimal("40"), new BigDecimal("50"), new BigDecimal("50"),
                DivergenceType.NONE, DivergenceType.NONE, DivergenceType.NONE,
                new BigDecimal(price), new BigDecimal("100"), new BigDecimal("100"), null);
    }

    private AnalyticsReport report(List<SignalSample> samples, List<Position> closed, List<Trade> trades) {
        LocalDateTime from = T0.minusMinutes(1);
        LocalDateTime to = T0.plusDays(1);
        return service.buildReport(MARKET, from, to, samples, closed, trades);
    }

    // ---------------- 전방수익 ----------------

    @Test
    void forwardReturn_usesTimestampsNotIndexArithmetic() {
        // 1분 간격 100개, 가격이 매 분 정확히 +1 (1000 → 1099).
        List<SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            samples.add(sample(i, 50, String.valueOf(1000 + i)));
        }

        List<TradingAnalyticsService.ForwardPoint> forward =
                TradingAnalyticsService.resolveForward(samples, 15);

        // i=0: (1015-1000)/1000 = 1.5%
        assertThat(forward.get(0).resolved()).isTrue();
        assertThat(forward.get(0).returnPct()).isEqualByComparingTo("1.5");
        assertThat(forward.get(50).returnPct()).isEqualByComparingTo("1.428571");   // (1065-1050)/1050

        // 꼬리 경계: i=85 의 목표는 100분인데 마지막 관측치가 99분이라 60초 차 — 허용오차(90초) 안이라
        // 근접값으로 채워진다. i=86 은 목표 101분에 대해 120초 차라 결측으로 버려진다.
        assertThat(forward.get(85).resolved()).isTrue();
        assertThat(forward.get(86).resolved()).isFalse();
        assertThat(forward.get(99).resolved()).isFalse();
    }

    @Test
    void forwardReturn_dropsRowWhenGapExceedsTolerance() {
        // t=0 관측치 하나, 그리고 t+15 주변이 통째로 비어 있고 t+20 에만 관측치가 있다.
        // 허용오차(90초)를 넘으므로 "가장 가까운 값" 을 억지로 쓰지 않고 버려야 한다.
        List<SignalSample> samples = List.of(
                sample(0, 50, "1000"),
                sample(20, 50, "1100"));

        List<TradingAnalyticsService.ForwardPoint> forward =
                TradingAnalyticsService.resolveForward(samples, 15);

        assertThat(forward.get(0).resolved()).isFalse();
    }

    @Test
    void forwardReturn_acceptsNeighbourWithinTolerance() {
        // t+15 정각은 없지만 t+16 (60초 차)은 허용오차 안이다.
        List<SignalSample> samples = List.of(
                sample(0, 50, "1000"),
                sample(16, 50, "1020"));

        List<TradingAnalyticsService.ForwardPoint> forward =
                TradingAnalyticsService.resolveForward(samples, 15);

        assertThat(forward.get(0).resolved()).isTrue();
        assertThat(forward.get(0).returnPct()).isEqualByComparingTo("2.0");
    }

    @Test
    void effectiveN_dividesResolvedCountByHorizon() {
        // 겹치는 창을 독립인 양 세면 안 된다. n=60 이어도 +15분 지평의 독립 관측은 4다.
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            returns.add(new BigDecimal("1.0"));
        }

        AnalyticsReport.HorizonStat stat =
                TradingAnalyticsService.stat(15, returns, new BigDecimal("0.5"));

        assertThat(stat.resolved()).isEqualTo(60);
        assertThat(stat.effectiveN()).isEqualTo(4);
        assertThat(stat.reliable()).isFalse();       // 4 < 30
        assertThat(stat.meanPct()).isEqualByComparingTo("1.0");
        assertThat(stat.netOfRoundTripFeePct()).isEqualByComparingTo("0.5");
    }

    @Test
    void episodes_countMaximalRunsNotRows() {
        // 5분 연속 + 공백 + 3분 연속 = 8행이지만 국면은 2개다.
        List<SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) samples.add(sample(i, 50, "1000"));
        for (int i = 5; i < 8; i++) samples.add(sample(i, 10, "1000"));
        for (int i = 8; i < 11; i++) samples.add(sample(i, 50, "1000"));

        long episodes = TradingAnalyticsService.countEpisodes(samples, s -> s.totalScore() >= 40);

        assertThat(episodes).isEqualTo(2);
    }

    @Test
    void scoreBuckets_placeThresholdOnABoundary() {
        // 40 이 구간 경계여야 "±40 임계가 맞는가" 를 물을 수 있다. 경계가 아니면 답이 뭉개진다.
        List<SignalSample> samples = List.of(
                sample(0, 39, "1000"),
                sample(1, 40, "1000"));

        AnalyticsReport r = report(samples, List.of(), List.of());

        AnalyticsReport.ScoreBucketRow lower = r.forwardReturnByScore().stream()
                .filter(b -> "20 ~ 40".equals(b.bucketLabel())).findFirst().orElseThrow();
        AnalyticsReport.ScoreBucketRow upper = r.forwardReturnByScore().stream()
                .filter(b -> "40 ~ 60".equals(b.bucketLabel())).findFirst().orElseThrow();

        assertThat(lower.n()).isEqualTo(1);
        assertThat(upper.n()).isEqualTo(1);
        assertThat(upper.containsCurrentBuyThreshold()).isTrue();
    }

    // ---------------- 커버리지 ----------------

    @Test
    void emptyWindowProducesEmptyReportNotException() {
        AnalyticsReport r = report(List.of(), List.of(), List.of());

        assertThat(r.isEmpty()).isTrue();
        assertThat(r.coverage().signalRows()).isZero();
        assertThat(r.coverage().coveragePct()).isEqualByComparingTo("0.00");
        assertThat(r.coverage().tooThinToConclude()).isTrue();
        assertThat(r.cost().feesAsPctOfNotional()).isNull();
        // 구간이 비어도 표 자체는 렌더된다 — 빈 구간도 "그 점수가 한 번도 안 나왔다" 는 정보다.
        assertThat(r.forwardReturnByScore()).isNotEmpty();
        assertThat(r.componentEdge()).hasSize(8);
    }

    @Test
    void thinSampleFlagsEachReasonWithActualNumbers() {
        List<SignalSample> samples = List.of(sample(0, 10, "1000"), sample(1, 10, "1000"));

        AnalyticsReport r = report(samples, List.of(), List.of());

        assertThat(r.coverage().signalSectionsSufficient()).isFalse();
        assertThat(r.coverage().outcomeSectionsSufficient()).isFalse();
        assertThat(r.coverage().thinReasons())
                .anyMatch(s -> s.contains("거래일"))
                .anyMatch(s -> s.contains("신호"))
                .anyMatch(s -> s.contains("청산 포지션"));
    }

    @Test
    void gapsAreCountedAndReportedAsCaveat() {
        List<SignalSample> samples = List.of(
                sample(0, 10, "1000"),
                sample(1, 10, "1000"),
                sample(30, 10, "1000"));   // 29분 공백

        AnalyticsReport r = report(samples, List.of(), List.of());

        assertThat(r.coverage().gapCount()).isEqualTo(1);
        assertThat(r.coverage().longestGapMinutes()).isEqualTo(29);
        assertThat(r.caveats()).anyMatch(c -> c.contains("공백"));
    }

    @Test
    void stochKEqualsStochDProbeIsMeasuredNotAsserted() {
        // 두 값이 같다는 것을 문장으로 주장하지 않고 데이터로 센다 — 설정이 바뀌면 수치가 따라간다.
        List<SignalSample> samples = List.of(sample(0, 10, "1000"), sample(1, 10, "1000"));

        AnalyticsReport r = report(samples, List.of(), List.of());

        assertThat(r.coverage().stochKEqualsStochDRows()).isEqualTo(2);
        assertThat(r.caveats()).anyMatch(c -> c.contains("stoch_d"));
    }

    // ---------------- 진입 맥락 조인 ----------------

    @Test
    void entryMatchUsesPrecedingSignalNotNearestOne() {
        // 신호는 T+0 과 T+60초, 포지션은 T+31초에 열렸다.
        // 대칭 최근접이면 T+60초(거리 29초)를 고른다 — 매매 뒤에 생성된 신호를 원인으로 기록하는 셈.
        List<SignalSample> samples = List.of(
                sample(0, 45, "1000"),
                sample(1, 90, "1000"));

        SignalSample matched = TradingAnalyticsService.precedingSignal(samples, T0.plusSeconds(31));

        assertThat(matched).isNotNull();
        assertThat(matched.totalScore()).isEqualTo(45);   // T+0 의 신호
    }

    @Test
    void entryMatchIgnoresSignalOlderThanWindow() {
        List<SignalSample> samples = List.of(sample(0, 45, "1000"));

        // 91초 전 신호는 창(90초) 밖이다.
        assertThat(TradingAnalyticsService.precedingSignal(samples, T0.plusSeconds(91))).isNull();
    }

    @Test
    void rebalanceEntriesAreExcludedNotSilentlyMixedIn() {
        List<SignalSample> samples = List.of(sample(0, 45, "1000"));

        Position signalPos = closedPosition(1L, T0.plusSeconds(5), "1000", "1020");
        Position rebalancePos = closedPosition(2L, T0.plusSeconds(5), "1000", "980");

        Trade signalBuy = buyTrade(1L, "Auto buy signal", 45);
        Trade rebalanceBuy = buyTrade(2L, "Rebalancing buy", null);

        AnalyticsReport r = report(samples, List.of(signalPos, rebalancePos),
                List.of(signalBuy, rebalanceBuy));

        assertThat(r.entryContext().closedPositions()).isEqualTo(2);
        assertThat(r.entryContext().excludedNonSignalPositions()).isEqualTo(1);
        assertThat(r.entryContext().signalDrivenPositions()).isEqualTo(1);
        assertThat(r.entryContext().scoreConfirmedMatches()).isEqualTo(1);
    }

    @Test
    void unmatchedPositionIsCountedNotDropped() {
        // 신호가 전혀 없는 구간에 열린 포지션.
        Position orphan = closedPosition(9L, T0.plusHours(5), "1000", "1010");

        AnalyticsReport r = report(List.of(sample(0, 45, "1000")), List.of(orphan),
                List.of(buyTrade(9L, "Auto buy signal", 45)));

        assertThat(r.entryContext().unmatched()).isEqualTo(1);
        assertThat(r.entryContext().closedPositions()).isEqualTo(1);
    }

    // ---------------- 비용 ----------------

    @Test
    void feesAsPctOfGrossIsNullOnALosingBook() {
        // 적자 장부에서 fees/gross 는 부호가 뒤집히거나 0 근처에서 발산한다 — 계산하지 않는다.
        Position loser = closedPosition(1L, T0, "1000", "900");

        AnalyticsReport r = report(List.of(), List.of(loser), List.of());

        assertThat(r.cost().netRealizedPnl().signum()).isNegative();
        assertThat(r.cost().feesAsPctOfGross()).isNull();
        assertThat(r.cost().feesAsPctOfNotional()).isNotNull();   // 항상 정의된다
    }

    @Test
    void breakEvenMoveIsTwiceTakerFee() {
        AnalyticsReport r = report(List.of(), List.of(), List.of());

        // taker-fee-rate 0.0025 → 왕복 0.50%
        assertThat(r.cost().breakEvenMovePct()).isEqualByComparingTo("0.50");
    }

    @Test
    void closeReasonBreakdownGroupsAndCountsWins() {
        Position win = closedPosition(1L, T0, "1000", "1100");
        Position loss = closedPosition(2L, T0, "1000", "900");

        AnalyticsReport r = report(List.of(), List.of(win, loss), List.of());

        AnalyticsReport.CloseReasonRow row = r.outcomeByCloseReason().stream()
                .filter(x -> x.closeReason().equals(CloseReason.SIGNAL.name()))
                .findFirst().orElseThrow();
        assertThat(row.n()).isEqualTo(2);
        assertThat(row.wins()).isEqualTo(1);
        assertThat(row.winRatePct()).isEqualByComparingTo("50.00");
    }

    // ---------------- 픽스처 ----------------

    /**
     * 청산된 포지션. 손익은 실제 {@code Position.close} 로 계산해두고(비용 모델을 손으로 베끼지
     * 않기 위해), {@code openedAt} 만 원하는 시각으로 바꿔 전체 생성자로 재구성한다 —
     * 도메인에 setter 가 없고 {@code open()} 은 {@code now()} 를 박기 때문이다.
     */
    private Position closedPosition(long id, LocalDateTime openedAt, String entry, String exit) {
        Position computed = Position.open(MARKET, new BigDecimal(entry), BigDecimal.TEN,
                new BigDecimal(entry), new BigDecimal(entry), new BigDecimal("25"));
        computed.close(new BigDecimal(exit), BigDecimal.TEN, CloseReason.SIGNAL, new BigDecimal("25"));

        return new Position(id, MARKET, computed.getStatus(),
                computed.getEntryPrice(), computed.getEntryVolume(), computed.getEntryAmount(),
                computed.getExitPrice(), computed.getExitVolume(), computed.getExitAmount(),
                computed.getRealizedPnl(), computed.getRealizedPnlPct(),
                computed.getStopLossPrice(), computed.getTakeProfitPrice(),
                computed.getTrailingStopPrice(), computed.getHighWaterMark(), computed.isTrailingStopActive(),
                computed.getCloseReason(), openedAt, openedAt.plusMinutes(20), openedAt,
                computed.getEntryFee(), computed.getExitFee(), computed.getTotalFees());
    }

    private Trade buyTrade(long positionId, String reason, Integer signalScore) {
        Trade t = Trade.createBuyOrder("uuid-" + positionId, MARKET, new BigDecimal("1000"),
                BigDecimal.TEN, "market", signalScore, reason);
        t.setPositionId(positionId);
        return t;
    }
}
