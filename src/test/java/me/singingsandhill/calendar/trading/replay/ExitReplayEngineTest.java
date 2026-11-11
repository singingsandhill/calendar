package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties.Bot.TimeExitMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 출구 리플레이 엔진 (ADR trading/observability/0005) — 진입 290원 × 10개, 기록 진입수수료 0, 판정 수수료 0.25%(운영 taker-fee-rate),
 * 손익 수수료 0.04% 양쪽. 판정 손익률은 운영과 같은 {@code Position.calculateUnrealizedPnlPctWithFee}.
 * 286 = -1.66%, 285 = -2.00%, 295 = +1.45%, 296 = +1.79%, 300 = +3.17%.
 */
class ExitReplayEngineTest {

    private static final LocalDateTime OPENED = LocalDateTime.of(2026, 8, 1, 10, 0, 30);
    private static final ExitReplayConfig HIGH_LOW = ExitReplayConfig.historical(PriceModel.HIGH_LOW, 0.0025, 0.0004);
    private static final ExitReplayConfig CLOSE = ExitReplayConfig.historical(PriceModel.CLOSE, 0.0025, 0.0004);

    private static ReplayPosition position(String actualReason, LocalDateTime closedAt, String exitPrice) {
        return new ReplayPosition(1L, OPENED, closedAt, new BigDecimal("290"), BigDecimal.TEN, BigDecimal.ZERO,
                new BigDecimal(exitPrice), actualReason, BigDecimal.ZERO);
    }

    private static ReplayPosition stoppedPosition() {
        return position("STOP_LOSS", OPENED.plusMinutes(5), "285");
    }

    private static ReplayBar bar(int hour, int minute, String o, String h, String l, String c) {
        return new ReplayBar(LocalDateTime.of(2026, 8, 1, hour, minute), new BigDecimal(o), new BigDecimal(h),
                new BigDecimal(l), new BigDecimal(c));
    }

    private static ReplayBar flat(int hour, int minute, String price) {
        return bar(hour, minute, price, price, price, price);
    }

    private static ReplaySignal signal(int hour, int minute, String type, int score, String price) {
        return new ReplaySignal(LocalDateTime.of(2026, 8, 1, hour, minute, 10), type, score, false, new BigDecimal(price));
    }

    private static MinuteSeries series(List<ReplaySignal> signals, ReplayBar... bars) {
        List<ReplayBar> list = new ArrayList<>(List.of(flat(10, 0, "290")));   // 진입 분 — 판정에서 제외
        list.addAll(List.of(bars));
        return new MinuteSeries(list, signals);
    }

    private static MinuteSeries series(ReplayBar... bars) {
        return series(List.of(), bars);
    }

    @Test
    void stopLossComesFirstWhenOneBarHitsBothStopAndTakeProfit() {
        ExitOutcome out = ExitReplayEngine.simulate(stoppedPosition(), series(bar(10, 1, "290", "300", "284", "290")), HIGH_LOW);

        assertThat(out.reason()).isEqualTo("STOP_LOSS");
        // 판정 손익률이 -1.5% 이하가 되는 가장 높은 호가 = 286 (-1.66%). 287 은 -1.31%
        assertThat(out.exitPrice()).isEqualByComparingTo("286");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 1));
        // (286 - 290) × 10 - 2,900 × 0.04% - 2,860 × 0.04% = -42.304
        assertThat(out.pnl()).isEqualByComparingTo("-42.304");
        assertThat(out.pnlPct()).isCloseTo(-42.304 / 2900 * 100, within(1e-9));
    }

    @Test
    void tickFloorMovesTheStopOneTickLower() {
        ExitReplayConfig floor = HIGH_LOW.withExits(0.008, 0.015, TimeExitMode.BREAKEVEN_CLOSE, 360, 5);

        assertThat(ExitReplayEngine.simulate(stoppedPosition(), series(bar(10, 1, "290", "290", "284", "288")), floor)
                .exitPrice()).isEqualByComparingTo("285");
        // 저가 286(-1.66%)은 유효 손절 -1.724% 안 — 손절 없이 데이터 끝까지
        assertThat(ExitReplayEngine.simulate(stoppedPosition(), series(bar(10, 1, "290", "290", "286", "288")), floor)
                .reason()).isEqualTo("END_OF_DATA");
    }

    @Test
    void trailingActivatedByABarHighAppliesFromTheNextBar() {
        MinuteSeries s = series(
                bar(10, 1, "290", "296", "290", "295"),   // +1.79% 로 활성화, 스탑 max(296 - 2.368 → 293, 손익분기 292) = 293
                bar(10, 2, "295", "295", "292", "293"));

        ExitOutcome out = ExitReplayEngine.simulate(position("TRAILING_STOP", OPENED.plusMinutes(2), "293"), s, HIGH_LOW);

        assertThat(out.reason()).isEqualTo("TRAILING_STOP");
        assertThat(out.exitPrice()).isEqualByComparingTo("293");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 2));
    }

    @Test
    void takeProfitFillsAtTheFirstGridPriceReachingTheTarget() {
        ExitOutcome out = ExitReplayEngine.simulate(position("TAKE_PROFIT", OPENED.plusMinutes(1), "300"),
                series(bar(10, 1, "290", "310", "289", "305")), HIGH_LOW);

        assertThat(out.reason()).isEqualTo("TAKE_PROFIT");
        assertThat(out.exitPrice()).isEqualByComparingTo("300");   // 299 = +2.83%, 300 = +3.17%
    }

    @Test
    void closeModelReadsOnlyTheClose() {
        MinuteSeries s = series(bar(10, 1, "290", "300", "284", "289"), flat(10, 2, "285"));

        ExitOutcome out = ExitReplayEngine.simulate(stoppedPosition(), s, CLOSE);

        assertThat(out.reason()).isEqualTo("STOP_LOSS");
        assertThat(out.exitPrice()).isEqualByComparingTo("285");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 2));
    }

    @Test
    void breakevenCloseExitsAtMaxHoldOnMinutesWithoutTrades() {
        // 10:01 이후 체결 없는 분은 직전 종가 295 로 메운다 — 16:00 봉의 판정 시각 16:01 에 보유 360분
        MinuteSeries s = series(flat(10, 1, "295"), flat(18, 0, "295"));

        ExitOutcome out = ExitReplayEngine.simulate(position("TIME_EXIT", OPENED.plusHours(6), "295"), s, HIGH_LOW);

        assertThat(out.reason()).isEqualTo("TIME_EXIT");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 16, 0));
        assertThat(out.exitPrice()).isEqualByComparingTo("295");
    }

    @Test
    void tightenTrailKeepsTheWinnerRunning() {
        // 295(+1.45%)는 활성화 +1.5% 미만. maxHold 에 트레일링을 max(295 - 2.36 → 292, 손익분기 292) = 292 로 켜고 계속 보유
        MinuteSeries s = series(flat(10, 1, "295"), bar(16, 1, "295", "300", "295", "300"));
        ExitReplayConfig tighten = HIGH_LOW.withExits(0.008, 0.015, TimeExitMode.TIGHTEN_TRAIL, 360, 0);

        ExitOutcome out = ExitReplayEngine.simulate(position("TIME_EXIT", OPENED.plusHours(6), "295"), s, tighten);

        assertThat(out.reason()).isEqualTo("TAKE_PROFIT");
        assertThat(out.exitPrice()).isEqualByComparingTo("300");
    }

    @Test
    void signalGatedWaitsForANonPositiveScoreSeenBeforeTheBar() {
        // 16:30:10 신호는 16:30 폴링의 리스크 판정 뒤에 생기므로 16:31 봉에서 처음 보인다
        List<ReplaySignal> signals = List.of(signal(15, 0, "HOLD", 10, "295"), signal(16, 30, "HOLD", -5, "295"));
        MinuteSeries s = series(signals, flat(10, 1, "295"), flat(18, 0, "295"));
        ExitReplayConfig gated = HIGH_LOW.withExits(0.008, 0.015, TimeExitMode.SIGNAL_GATED, 360, 0);

        ExitOutcome out = ExitReplayEngine.simulate(position("TIME_EXIT", OPENED.plusHours(6), "295"), s, gated);

        assertThat(out.reason()).isEqualTo("TIME_EXIT");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 16, 31));
    }

    @Test
    void signalSellNeedsMinHoldingAndTheNetMarginGate() {
        // 296 에서 트레일링이 켜져도(스탑 293) 가격이 버티는 동안 — 10:10 매도 신호는 보유 30분 미만이라 무시, 10:40 은 체결
        List<ReplaySignal> signals = List.of(signal(10, 10, "SELL", -45, "296"), signal(10, 40, "SELL", -45, "296"));
        MinuteSeries s = series(signals, flat(10, 1, "296"), flat(12, 0, "296"));

        ExitOutcome out = ExitReplayEngine.simulate(position("SIGNAL", OPENED.plusMinutes(40), "296"), s, HIGH_LOW);

        assertThat(out.reason()).isEqualTo("SIGNAL");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 40));
        assertThat(out.exitPrice()).isEqualByComparingTo("296");
    }

    @Test
    void onlyAStrongSellSignalAcceptsASmallLoss() {
        // 289 × (1 - 0.5%) 의 판정 손익률 -1.12%: 강한 신호(-60 이하)는 -2% 까지 허용, 일반 신호는 +0.1% 필요
        MinuteSeries weak = series(List.of(signal(10, 40, "SELL", -45, "289")), flat(10, 1, "289"), flat(12, 0, "289"));
        MinuteSeries strong = series(List.of(signal(10, 40, "SELL", -70, "289")), flat(10, 1, "289"), flat(12, 0, "289"));
        ReplayPosition p = position("STOP_LOSS", OPENED.plusHours(3), "285");

        assertThat(ExitReplayEngine.simulate(p, weak, HIGH_LOW).reason()).isEqualTo("END_OF_DATA");
        assertThat(ExitReplayEngine.simulate(p, strong, HIGH_LOW).reason()).isEqualTo("SIGNAL");
    }

    @Test
    void anExitTheReplayDoesNotModelEndsASurvivingPositionAtTheActualPrice() {
        ReplayPosition rebalanced = position("REBALANCE", LocalDateTime.of(2026, 8, 1, 11, 0, 20), "291");

        ExitOutcome out = ExitReplayEngine.simulate(rebalanced, series(flat(10, 1, "290"), flat(12, 0, "290")), HIGH_LOW);

        assertThat(out.reason()).isEqualTo("REBALANCE");
        assertThat(out.exitTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 11, 0));
        assertThat(out.exitPrice()).isEqualByComparingTo("291");
    }

    @Test
    void positionsOutsideTheSeriesAreNotSimulated() {
        ReplayPosition early = new ReplayPosition(2L, OPENED.minusDays(1), OPENED, new BigDecimal("290"), BigDecimal.TEN,
                BigDecimal.ZERO, new BigDecimal("290"), "STOP_LOSS", BigDecimal.ZERO);

        assertThat(ExitReplayEngine.simulate(early, series(flat(10, 1, "290")), HIGH_LOW)).isNull();
    }
}
