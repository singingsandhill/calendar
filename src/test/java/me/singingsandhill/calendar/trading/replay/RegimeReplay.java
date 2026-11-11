package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimeCalculator;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 레짐 필터 평가(작업 6-B) — 실제 청산 포지션을 진입 시점의 일봉 레짐으로 나누고, 필터 모드별로 걸렀을 때의 가상 결과를 낸다.
 * 진입일 D 의 레짐 = D-1 종가까지의 판정(운영과 같은 {@code RegimeCalculator.regimeInEffectOn}) — 미래 데이터 금지.
 * BLOCK = 그 진입을 뺀다, SCALE = 손익 × 배수. 뺀 진입이 풀어 준 자본·다른 매매에 대한 파급은 반영하지 않는다.
 */
final class RegimeReplay {

    /** 필터 모드 × UNKNOWN 정책 (운영 filter-mode × unknown-policy). OFF = 필터 없음(기준선). */
    enum Variant { OFF, BLOCK_BEAR, BLOCK_BEAR_ALLOW_UNKNOWN, SCALE_BEAR, SCALE_BEAR_ALLOW_UNKNOWN }

    private RegimeReplay() {
    }

    static MarketRegime regimeAtEntry(List<RegimePoint> points, ReplayTrade trade) {
        return RegimeCalculator.regimeInEffectOn(points, trade.openedAt().toLocalDate());
    }

    /**
     * 4-D 필터 판정 (ADR trading/risk/0007) — 두 구간 모두에서 BEAR 진입 기대값 < BULL 진입 기대값이고, 필터를 적용하면 총손익이
     * 늘고 MDD 가 나빠지지 않을 때만. 한 구간에 BULL·BEAR 진입이 없으면 근거 부족으로 본다.
     */
    static boolean improvesBothPeriods(Variant variant, double bearMultiplier, List<RegimePoint> points,
                                       List<ReplayTrade> first, List<ReplayTrade> second) {
        return variant != Variant.OFF && improves(variant, bearMultiplier, points, first)
                && improves(variant, bearMultiplier, points, second);
    }

    private static boolean improves(Variant variant, double bearMultiplier, List<RegimePoint> points, List<ReplayTrade> trades) {
        ReplayStats bull = ReplayStats.of(trades.stream().filter(t -> regimeAtEntry(points, t) == MarketRegime.BULL).toList());
        ReplayStats bear = ReplayStats.of(trades.stream().filter(t -> regimeAtEntry(points, t) == MarketRegime.BEAR).toList());
        if (bull.count() == 0 || bear.count() == 0 || bear.expectancyPct() >= bull.expectancyPct()) {
            return false;
        }
        ReplayStats off = ReplayStats.of(trades);
        ReplayStats on = ReplayStats.of(apply(variant, bearMultiplier, points, trades));
        return on.total().compareTo(off.total()) > 0 && on.maxDrawdown().compareTo(off.maxDrawdown()) <= 0;
    }

    static List<ReplayTrade> apply(Variant variant, double bearMultiplier, List<RegimePoint> points, List<ReplayTrade> trades) {
        List<ReplayTrade> kept = new ArrayList<>();
        for (ReplayTrade t : trades) {
            MarketRegime regime = regimeAtEntry(points, t);
            boolean unknownAllowed = variant == Variant.BLOCK_BEAR_ALLOW_UNKNOWN || variant == Variant.SCALE_BEAR_ALLOW_UNKNOWN;
            if (variant == Variant.OFF || regime == MarketRegime.BULL) {
                kept.add(t);
            } else if (regime == MarketRegime.UNKNOWN) {
                if (unknownAllowed) {
                    kept.add(t);
                }
            } else if (variant == Variant.SCALE_BEAR || variant == Variant.SCALE_BEAR_ALLOW_UNKNOWN) {
                kept.add(t.scaled(bearMultiplier));
            }
        }
        return kept;
    }
}
