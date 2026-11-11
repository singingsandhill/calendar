package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 건수·승률·평균 이익/손실(%)·기대값(건당 평균 %)·총손익(원)·MDD(원)·출구 사유 분포.
 * MDD = 청산 시각 순 누적손익(0 에서 출발)의 고점 대비 최대 하락폭.
 */
record ReplayStats(int count, int wins, double winRatePct, double avgWinPct, double avgLossPct, double expectancyPct,
                   BigDecimal total, BigDecimal maxDrawdown, Map<String, Integer> reasons) {

    static ReplayStats of(List<ReplayTrade> trades) {
        List<ReplayTrade> ordered = trades.stream().sorted(Comparator.comparing(ReplayTrade::closedAt)).toList();
        int wins = 0;
        int losses = 0;
        double winSum = 0;
        double lossSum = 0;
        double pctSum = 0;
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        Map<String, Integer> reasons = new TreeMap<>();
        for (ReplayTrade t : ordered) {
            if (t.pnl().signum() > 0) {
                wins++;
                winSum += t.pnlPct();
            } else if (t.pnl().signum() < 0) {
                losses++;
                lossSum += t.pnlPct();
            }
            pctSum += t.pnlPct();
            cumulative = cumulative.add(t.pnl());
            peak = peak.max(cumulative);
            maxDrawdown = maxDrawdown.max(peak.subtract(cumulative));
            reasons.merge(t.reason(), 1, Integer::sum);
        }
        int n = ordered.size();
        return new ReplayStats(n, wins, n == 0 ? 0 : wins * 100.0 / n, wins == 0 ? 0 : winSum / wins,
                losses == 0 ? 0 : lossSum / losses, n == 0 ? 0 : pctSum / n, cumulative, maxDrawdown, reasons);
    }
}
