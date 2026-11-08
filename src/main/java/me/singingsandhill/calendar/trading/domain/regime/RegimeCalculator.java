package me.singingsandhill.calendar.trading.domain.regime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 일봉 레짐 판정 — 종가 vs 단순이동평균 + 히스테리시스 밴드 + 최소 유지 일수 (ADR trading/risk/0007).
 *
 * 운영({@code DailyRegimeService})과 리플레이(작업 6-B·6-C)가 같은 함수를 쓴다 — 판정 규칙이 두 벌이면 리플레이로
 * 고른 값이 운영에서 다르게 동작한다. 상태는 경로 의존적이라(히스테리시스·최소 유지) 항상 처음부터 순서대로 계산한다.
 *
 * <ul>
 *   <li>이동평균을 낼 수 있는 첫 날: 종가 ≥ MA 면 BULL, 아니면 BEAR (밴드 없이 초기화)</li>
 *   <li>이후: 종가 > MA × (1 + h) 면 BULL 신호, 종가 < MA × (1 − h) 면 BEAR 신호, 밴드 안이면 현 상태 유지</li>
 *   <li>신호가 현 상태와 달라도, 현 상태로 지낸 날이 minDays 미만이면 전환하지 않는다</li>
 * </ul>
 */
public final class RegimeCalculator {

    private RegimeCalculator() {
    }

    /**
     * @param barsAscending 확정 일봉 (날짜 오름차순 — 형성 중인 당일봉은 호출측이 뺀다)
     * @param maDays        단순이동평균 일수
     * @param hysteresisPct 밴드 폭 (0.02 = MA ± 2%)
     * @param minDays       한 상태의 최소 유지 일수 (1 이면 매일 전환 가능)
     * @return 날짜별 판정 (입력과 같은 길이·순서)
     */
    public static List<RegimePoint> compute(List<DailyBar> barsAscending, int maDays, double hysteresisPct,
                                            int minDays) {
        List<RegimePoint> points = new ArrayList<>(barsAscending.size());
        BigDecimal upperFactor = BigDecimal.ONE.add(BigDecimal.valueOf(hysteresisPct));
        BigDecimal lowerFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(hysteresisPct));
        MarketRegime state = MarketRegime.UNKNOWN;
        int heldDays = 0;
        BigDecimal windowSum = BigDecimal.ZERO;

        for (int i = 0; i < barsAscending.size(); i++) {
            DailyBar bar = barsAscending.get(i);
            windowSum = windowSum.add(bar.close());
            if (i >= maDays) {
                windowSum = windowSum.subtract(barsAscending.get(i - maDays).close());
            }
            if (i + 1 < maDays) {
                points.add(new RegimePoint(bar.date(), MarketRegime.UNKNOWN, bar.close(), null));
                continue;
            }
            BigDecimal ma = windowSum.divide(BigDecimal.valueOf(maDays), 8, RoundingMode.HALF_UP);

            if (state == MarketRegime.UNKNOWN) {
                state = bar.close().compareTo(ma) >= 0 ? MarketRegime.BULL : MarketRegime.BEAR;
                heldDays = 1;
            } else {
                MarketRegime signal = state;
                if (bar.close().compareTo(ma.multiply(upperFactor)) > 0) {
                    signal = MarketRegime.BULL;
                } else if (bar.close().compareTo(ma.multiply(lowerFactor)) < 0) {
                    signal = MarketRegime.BEAR;
                }
                if (signal != state && heldDays >= minDays) {
                    state = signal;
                    heldDays = 1;
                } else {
                    heldDays++;
                }
            }
            points.add(new RegimePoint(bar.date(), state, bar.close(), ma));
        }
        return points;
    }

    /**
     * day 에 적용되는 레짐 = day 전날 종가까지의 판정. 진입 시점에 미래(당일 종가)를 보지 않는다.
     * 전날 판정이 없으면(데이터 범위 밖·결측) UNKNOWN.
     */
    public static MarketRegime regimeInEffectOn(List<RegimePoint> points, LocalDate day) {
        LocalDate previous = day.minusDays(1);
        for (int i = points.size() - 1; i >= 0; i--) {
            RegimePoint point = points.get(i);
            if (point.date().equals(previous)) {
                return point.regime();
            }
            if (point.date().isBefore(previous)) {
                break;
            }
        }
        return MarketRegime.UNKNOWN;
    }
}
