package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 리밸런싱 엔진 일봉 리플레이(작업 6-C) — ADR trading/risk/0007 4-C 규칙: 일봉 레짐 목표(BULL 70%/BEAR 30%), 편차 10%,
 * 하루 한 번 시가 체결, 매도는 FIFO 로 포지션 통째, 다른 레짐에서 산 포지션(전환 매도)은 수익 가드 무시, 비상 손절 -15% 후
 * 레짐이 바뀌기 전까지 매수 차단. 수수료 0.04% 양쪽, 시작 100만원.
 */
class RebalanceReplayTest {

    private static final LocalDate D0 = LocalDate.of(2026, 1, 1);

    private static RebalanceReplay.Settings settings(double deviation, double catastrophic) {
        return new RebalanceReplay.Settings(0.70, 0.30, deviation, 5000, 0.005, catastrophic, 0.0, 0.0025, 0.0004,
                new BigDecimal("1000000"));
    }

    private static DailyBar bar(int day, String open, String high, String low, String close) {
        return new DailyBar(D0.plusDays(day), new BigDecimal(open), new BigDecimal(high), new BigDecimal(low),
                new BigDecimal(close));
    }

    private static DailyBar flat(int day, String price) {
        return bar(day, price, price, price, price);
    }

    /** regimes[i] = D0+i 종가까지의 판정 (그다음 날에 적용). */
    private static List<RegimePoint> points(MarketRegime... regimes) {
        List<RegimePoint> points = new ArrayList<>();
        for (int i = 0; i < regimes.length; i++) {
            points.add(new RegimePoint(D0.plusDays(i), regimes[i], BigDecimal.ONE, BigDecimal.ONE));
        }
        return points;
    }

    private static List<String> actions(RebalanceReplay.Result result) {
        return result.events().stream().map(e -> e.date().getDayOfMonth() + ":" + e.action()).toList();
    }

    @Test
    void regimeSwitchSellIgnoresTheProfitGuard() {
        List<DailyBar> bars = List.of(flat(1, "100"), flat(2, "95"));

        RebalanceReplay.Result result = RebalanceReplay.run(bars,
                points(MarketRegime.BULL, MarketRegime.BEAR, MarketRegime.BEAR), D0.plusDays(1), settings(0.10, -0.15));

        // 1/2 BULL: 70만 × 0.995 = 696,500원 → 6,965개 @100. 1/3 BEAR 전환: 손실(-5%)이어도 통째로 매도
        assertThat(actions(result)).containsExactly("2:BUY", "3:SELL");
        assertThat(result.events().get(1).volume()).isEqualByComparingTo("6965");
        // 1,000,000 - 696,500 - 278.6 + 661,675 - 264.67
        assertThat(result.finalEquity()).isEqualByComparingTo("964631.73");
        assertThat(result.trades()).isEqualTo(2);
    }

    @Test
    void sameRegimeDeviationSellKeepsTheProfitGuard() {
        // 1/2 A 매수 @100, 1/3 @60 에서 70% 로 B 추가 매수, 1/4 @70 비중 초과 매도 — A(-30%)는 같은 레짐이라 건너뛰고 B 만 판다
        List<DailyBar> bars = List.of(flat(1, "100"), flat(2, "60"), flat(3, "70"));

        RebalanceReplay.Result result = RebalanceReplay.run(bars,
                points(MarketRegime.BULL, MarketRegime.BULL, MarketRegime.BULL, MarketRegime.BULL), D0.plusDays(1),
                settings(0.01, -0.99));

        assertThat(actions(result)).containsExactly("2:BUY", "3:BUY", "4:SELL");
        assertThat(result.events().get(2).volume()).isEqualByComparingTo(result.events().get(1).volume());
    }

    @Test
    void catastrophicStopBlocksRebalanceBuysUntilTheRegimeChanges() {
        List<DailyBar> bars = List.of(flat(1, "100"), bar(2, "90", "90", "80", "85"), flat(3, "85"), flat(4, "85"));

        RebalanceReplay.Result result = RebalanceReplay.run(bars,
                points(MarketRegime.BULL, MarketRegime.BULL, MarketRegime.BULL, MarketRegime.BEAR, MarketRegime.BEAR),
                D0.plusDays(1), settings(0.10, -0.15));

        assertThat(actions(result)).containsExactly("2:BUY", "3:CATASTROPHIC_STOP", "4:BLOCKED", "5:BUY");
        // 판정 손익률(진입수수료 278.6 + 청산 0.25% 추정)이 -15% 이하가 되는 가장 높은 호가 85.25 (100원 미만은 0.01원 단위)
        assertThat(result.events().get(1).price()).isEqualByComparingTo("85.25");
        assertThat(result.catastrophicStops()).isEqualTo(1);
    }

    @Test
    void buyAndHoldMarksToMarketWithTheEntryFee() {
        List<DailyBar> bars = List.of(bar(1, "100", "100", "100", "100"), flat(2, "50"), flat(3, "120"));

        RebalanceReplay.Result result = RebalanceReplay.buyAndHold(bars, D0.plusDays(1), 0.0004, new BigDecimal("1000000"));

        // 1,000,000 / 1.0004 = 999,600.16 → 9,996.0016개. 최종 × 120, 낙폭은 시작자본 100만 → 499,800 (50.02%)
        assertThat(result.totalReturnPct()).isCloseTo(19.952, within(0.001));
        assertThat(result.maxDrawdownPct()).isCloseTo(50.02, within(0.01));
        assertThat(result.trades()).isEqualTo(1);
    }
}
