package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimeCalculator;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 작업 6-B(레짐 필터 평가)·6-C(리밸런싱 엔진 일봉 리플레이) 보고서. 레짐은 운영과 같은 {@code RegimeCalculator}, 그리드는
 * ma-days {10, 20, 60} × hysteresis {0, 2, 5}% × min-days {1, 3}. 판정 기준은 ADR trading/risk/0007 4-D.
 */
final class RegimeReplayRunner {

    static final int[] MA_DAYS = {10, 20, 60};
    static final double[] HYSTERESIS = {0.0, 0.02, 0.05};
    static final int[] MIN_DAYS = {1, 3};
    private static final double BEAR_MULTIPLIER = new TradingProperties().getRegime().getBearSizeMultiplier();
    private static final BigDecimal INITIAL_KRW = new BigDecimal("1000000");

    /** positions 가 null 이면 DB 사본 없음 → 6-B 건너뜀. */
    record Input(List<DailyBar> dailyBars, List<ReplayPosition> positions, int excluded, String dbLabel, String zoneLabel,
                 ReplaySettings settings) {
    }

    private record Grid(int maDays, double hysteresis, int minDays) {
        String label() {
            return "ma " + maDays + " · h " + (int) Math.round(hysteresis * 100) + "% · min " + minDays;
        }

        List<RegimePoint> points(List<DailyBar> bars) {
            return RegimeCalculator.compute(bars, maDays, hysteresis, minDays);
        }
    }

    private RegimeReplayRunner() {
    }

    static List<Grid> grid() {
        List<Grid> grid = new ArrayList<>();
        for (int ma : MA_DAYS) {
            for (double h : HYSTERESIS) {
                for (int min : MIN_DAYS) {
                    grid.add(new Grid(ma, h, min));
                }
            }
        }
        return grid;
    }

    static String report(Input in) {
        ReplaySettings s = in.settings();
        List<DailyBar> bars = in.dailyBars();
        StringBuilder md = new StringBuilder();
        md.append("# 코인 레짐 리플레이 (작업 6-B·6-C) — ").append(s.reportDate()).append("\n\n");
        md.append("> `./gradlew replay` 가 생성한 보고서 (ADR trading/observability/0005). 레짐 판정은 운영과 같은 `RegimeCalculator`")
                .append(" — 진입일 D 에는 D-1 종가까지의 판정만 쓴다(미래 데이터 금지).\n\n");
        md.append("## 입력\n\n");
        md.append(Markdown.table(List.of("항목", "값"), List.of(
                List.of("일봉", s.market() + " 빗썸 공개 API " + bars.size() + "개 (" + bars.get(0).date() + " ~ "
                        + bars.get(bars.size() - 1).date() + ", 캐시 `" + slash(s.cache()) + "`, 형성 중 오늘 봉 제외)"),
                List.of("DB 사본", in.positions() == null ? "없음 (6-B 건너뜀)" : in.dbLabel() + " · DB 시각 - KST " + in.zoneLabel()),
                List.of("수수료", "손익 " + s.fee() * 100 + "% 양쪽 · 판정 " + s.decisionFee() * 100 + "%"))));
        appendRegimeFilter(md, in);
        appendRebalance(md, in);
        return md.toString();
    }

    private static void appendRegimeFilter(StringBuilder md, Input in) {
        ReplaySettings s = in.settings();
        md.append("\n## B. 레짐 필터 평가 (작업 4-D 근거)\n\n");
        if (in.positions() == null) {
            md.append("운영 DB 사본이 없어 건너뛰었다. 실행: `./gradlew replay -Dreplay.db=<scheduledb 사본 경로>`.\n");
            return;
        }
        List<ReplayTrade> trades = in.positions().stream().map(p -> {
            BigDecimal pnl = ExitReplayEngine.netPnl(p.entryPrice(), p.exitPrice(), p.volume(), s.fee());
            return new ReplayTrade(p.openedAt(), p.closedAt(), p.closeReason(), pnl,
                    pnl.doubleValue() / p.entryPrice().multiply(p.volume()).doubleValue() * 100);
        }).toList();
        md.append("실제 청산 포지션 ").append(trades.size()).append("건(진입 ").append(s.regimeFrom()).append(" 이후, PAPER 판별 제외 ")
                .append(in.excluded()).append("건)을 진입 시점 레짐으로 나눴다. BLOCK = 그 진입 제외, SCALE = 손익 × ")
                .append(BEAR_MULTIPLIER).append(". 손익은 실제 청산가를 모델 수수료로 다시 계산한 값. 뺀 진입이 풀어 준 자본·다른 매매로의 파급은")
                .append(" 반영하지 않는다.\n\n");
        md.append("- 구간 1: ").append(s.regimeFrom()).append(" ~ ").append(s.regimeSplit().minusDays(1)).append("\n");
        md.append("- 구간 2: ").append(s.regimeSplit()).append(" ~\n\n");
        md.append("리밸런싱 포지션은 2026-06-05 부터 생겼다(그 전 진입은 전부 신호 진입). 그 이후 진입 중 `entry_source` 가 없는 과거분은")
                .append(" 리밸런싱 진입이 섞였을 수 있다(2026-10-22 이전 포지션은 출처 미기록, ADR trading/observability/0004).\n\n");
        Predicate<ReplayTrade> first = t -> t.openedAt().toLocalDate().isBefore(s.regimeSplit());
        List<ReplayTrade> p1 = trades.stream().filter(first).toList();
        List<ReplayTrade> p2 = trades.stream().filter(first.negate()).toList();

        List<String> improvedBoth = new ArrayList<>();
        for (Map.Entry<String, List<ReplayTrade>> period : List.of(Map.entry("구간 1", p1), Map.entry("구간 2", p2))) {
            md.append("### ").append(period.getKey()).append(" (").append(period.getValue().size()).append("건)\n\n");
            List<List<String>> rows = new ArrayList<>();
            for (Grid g : grid()) {
                List<RegimePoint> points = g.points(in.dailyBars());
                Map<MarketRegime, Integer> counts = new EnumMap<>(MarketRegime.class);
                period.getValue().forEach(t -> counts.merge(RegimeReplay.regimeAtEntry(points, t), 1, Integer::sum));
                ReplayStats off = ReplayStats.of(period.getValue());
                List<String> row = new ArrayList<>(List.of(g.label(), counts.getOrDefault(MarketRegime.BULL, 0) + "/"
                        + counts.getOrDefault(MarketRegime.BEAR, 0) + "/" + counts.getOrDefault(MarketRegime.UNKNOWN, 0),
                        Markdown.pct(expectancy(points, period.getValue(), MarketRegime.BULL)) + " / "
                                + Markdown.pct(expectancy(points, period.getValue(), MarketRegime.BEAR)),
                        Markdown.won(off.total()) + " / " + Markdown.won(off.maxDrawdown())));
                for (RegimeReplay.Variant v : RegimeReplay.Variant.values()) {
                    if (v == RegimeReplay.Variant.OFF) {
                        continue;
                    }
                    ReplayStats st = ReplayStats.of(RegimeReplay.apply(v, BEAR_MULTIPLIER, points, period.getValue()));
                    row.add(Markdown.won(st.total()) + " (" + signedWon(st.total().subtract(off.total())) + ") / "
                            + Markdown.won(st.maxDrawdown()));
                }
                rows.add(row);
            }
            md.append(Markdown.table(List.of("레짐 설정", "진입 BULL/BEAR/UNKNOWN", "기대값 BULL / BEAR", "OFF 총손익 / MDD",
                    "BLOCK_BEAR", "BLOCK_BEAR+ALLOW", "SCALE_BEAR", "SCALE_BEAR+ALLOW"), rows));
            md.append("\n칸 = 총손익(OFF 대비) / MDD, 원. +ALLOW = UNKNOWN 진입 허용(unknown-policy ALLOW).\n\n");
        }
        for (Grid g : grid()) {
            List<RegimePoint> points = g.points(in.dailyBars());
            for (RegimeReplay.Variant v : RegimeReplay.Variant.values()) {
                if (RegimeReplay.improvesBothPeriods(v, BEAR_MULTIPLIER, points, p1, p2)) {
                    BigDecimal gain = total(RegimeReplay.apply(v, BEAR_MULTIPLIER, points, p1)).subtract(total(p1))
                            .add(total(RegimeReplay.apply(v, BEAR_MULTIPLIER, points, p2)).subtract(total(p2)));
                    improvedBoth.add(g.label() + " · " + v + " (두 구간 합계 " + signedWon(gain) + "원)");
                }
            }
        }
        md.append("### 판정 (4-D: 두 구간 모두 BEAR 진입 기대값 < BULL 이고, 필터 적용 시 총손익 증가·MDD 비악화일 때만)\n\n");
        md.append(improvedBoth.isEmpty() ? "조건을 채운 조합이 없다 — `filter-mode` 는 OFF 유지.\n"
                : "조건을 채운 조합 " + improvedBoth.size() + "개: " + String.join(", ", improvedBoth)
                + ". BLOCK_BEAR·SCALE_BEAR 중 합계가 큰 쪽을 제안하되, 레짐 설정(`trading.regime.*`)은 리밸런싱과 공유하므로 C 와 함께 본다.\n");
    }

    private static double expectancy(List<RegimePoint> points, List<ReplayTrade> trades, MarketRegime regime) {
        return ReplayStats.of(trades.stream().filter(t -> RegimeReplay.regimeAtEntry(points, t) == regime).toList())
                .expectancyPct();
    }

    private static void appendRebalance(StringBuilder md, Input in) {
        ReplaySettings s = in.settings();
        TradingProperties.Rebalancing r = new TradingProperties().getRebalancing();
        RebalanceReplay.Settings settings = new RebalanceReplay.Settings(r.getBullRatio(), r.getBearRatio(),
                r.getDeviationTrigger(), r.getMinOrderAmount(), r.getSlippageBuffer(), r.getCatastrophicStopPct(),
                r.getMinSellPnlPct(), s.decisionFee(), s.fee(), INITIAL_KRW);
        LocalDate from = s.dailyFrom();
        RebalanceReplay.Result hold = RebalanceReplay.buyAndHold(in.dailyBars(), from, s.fee(), INITIAL_KRW);
        md.append("\n## C. 리밸런싱 엔진 일봉 리플레이 (작업 4-C 재활성화 판정, ").append(from).append(" ~)\n\n");
        md.append("엔진을 켠 상태의 4-C 규칙(목표 BULL ").append((int) (r.getBullRatio() * 100)).append("% / BEAR ")
                .append((int) (r.getBearRatio() * 100)).append("%, 편차 ").append((int) (r.getDeviationTrigger() * 100))
                .append("%, 하루 한 번 시가 체결 — 운영 쿨다운 8시간의 근사, 전환 매도 가드 무시, 비상 손절 ")
                .append((int) (r.getCatastrophicStopPct() * 100)).append("% 후 레짐 전환 전 매수 차단)을 시작자본 100만원으로 돌렸다.\n\n");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("KRW 보유", "+0.00%", "0.0%", "—", "0", "0", "—", "—"));
        rows.add(List.of("단순 보유", Markdown.pct(hold.totalReturnPct()), Markdown.plainPct(hold.maxDrawdownPct()),
                ratio(hold), "1", "0", "—", "—"));
        List<Map.Entry<String, RebalanceReplay.Result>> winners = new ArrayList<>();
        for (Grid g : grid()) {
            RebalanceReplay.Result res = RebalanceReplay.run(in.dailyBars(), g.points(in.dailyBars()), from, settings);
            boolean beatsKrw = res.totalReturnPct() > 0;
            boolean beatsHold = riskAdjusted(res) > riskAdjusted(hold);
            if (beatsKrw && beatsHold) {
                winners.add(Map.entry(g.label(), res));
            }
            rows.add(List.of("엔진 " + g.label(), Markdown.pct(res.totalReturnPct()), Markdown.plainPct(res.maxDrawdownPct()),
                    ratio(res), String.valueOf(res.trades()), String.valueOf(res.catastrophicStops()), beatsKrw ? "✓" : "✗",
                    beatsHold ? "✓" : "✗"));
        }
        md.append(Markdown.table(List.of("전략", "총수익", "MDD", "총수익÷MDD", "매매", "비상 손절", "KRW 보유 대비", "단순 보유 대비"),
                rows));
        md.append("\n### 판정 (4-D: KRW 보유·단순 보유를 위험조정 수익으로 모두 이길 때만 재활성화)\n\n");
        if (winners.isEmpty()) {
            md.append("둘 다 이긴 설정이 없다 — `rebalancing.enabled` 는 false 유지.\n");
        } else {
            winners.sort(Comparator.comparingDouble((Map.Entry<String, RebalanceReplay.Result> e) -> riskAdjusted(e.getValue()))
                    .reversed());
            md.append("둘 다 이긴 설정 ").append(winners.size()).append("/").append(grid().size()).append("개 (총수익÷MDD 순): ")
                    .append(winners.stream().map(e -> e.getKey() + " " + Markdown.pct(e.getValue().totalReturnPct()) + " / MDD "
                            + Markdown.plainPct(e.getValue().maxDrawdownPct())).collect(Collectors.joining(", ")))
                    .append(".\n\n주의: ① KRW 보유는 수익·낙폭이 모두 0 이라 \"위험조정으로 이긴다\" 는 총수익 > 0 과 같아진다 — 이긴 설정도 MDD 가")
                    .append(" 수십 % 다. ② 일봉 한 기간(같은 기간 단순 보유 MDD ").append(Markdown.plainPct(hold.maxDrawdownPct()))
                    .append(")의 결과이고 18개 중 고른 값이라 과최적화 위험이 크다.")
                    .append(" ③ 하루 한 번 시가 체결·신호 매매와의 상호작용 없음 — 운영 엔진(분 단위, 쿨다운 8시간)과 다르다.")
                    .append(" ④ 레짐 설정(`trading.regime.*`)은 신호 매수 필터와 공유하고, 필터와 리밸런싱을 함께 켜는 조합은 제안하지 않는다(4-D).\n");
        }
        md.append("\nKRW 보유 대비 = 총수익 > 0, 단순 보유 대비 = 총수익÷MDD 가 더 큼(단순 보유가 손실이면 음수끼리 비교).\n");
    }

    static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static double riskAdjusted(RebalanceReplay.Result r) {
        return r.maxDrawdownPct() == 0 ? r.totalReturnPct() : r.totalReturnPct() / r.maxDrawdownPct();
    }

    private static String ratio(RebalanceReplay.Result r) {
        return r.maxDrawdownPct() == 0 ? "—" : String.format("%.2f", r.totalReturnPct() / r.maxDrawdownPct());
    }

    private static BigDecimal total(List<ReplayTrade> trades) {
        return trades.stream().map(ReplayTrade::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String signedWon(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + Markdown.won(value);
    }
}
