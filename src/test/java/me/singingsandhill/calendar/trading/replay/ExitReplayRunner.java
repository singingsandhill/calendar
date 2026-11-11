package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties.Bot.TimeExitMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 작업 6-A 보고서 — 보정 게이트(현행 설정이 실제를 재현하는가) → 통과한 가격 모델로만 그리드 → 상위 3 + 7~8월 선택·9~10월 검증.
 * 게이트를 하나도 통과하지 못하면 그리드를 돌리지 않고 원인 후보를 적고 멈춘다 (작업 6 정의).
 */
final class ExitReplayRunner {

    static final double GATE_PNL = 0.15;
    static final double GATE_STOPS = 0.10;
    static final double[] TRAILING = {0.008, 0.012, 0.017, 0.025};
    static final double[] ACTIVATION = {0.015, 0.020};
    static final long[] MAX_HOLD = {360, 720, 1440};
    static final int[] TICKS = {5, 0};

    record Input(List<ReplayPosition> positions, int excluded, MinuteSeries series, int signalCount, String dbLabel,
                 String zoneLabel, ReplaySettings settings) {
    }

    record Calibration(PriceModel model, ReplayStats replay, boolean passed, double pnlGapPct, double stopGapPct) {
    }

    private record Row(ExitReplayConfig config, ReplayStats all, ReplayStats firstHalf, ReplayStats secondHalf) {
    }

    private ExitReplayRunner() {
    }

    static String report(Input in) {
        ReplaySettings s = in.settings();
        List<ReplayPosition> covered = in.positions().stream().filter(p -> ExitReplayEngine.covers(in.series(), p)).toList();
        List<ReplayTrade> actual = covered.stream().map(p -> new ReplayTrade(p.openedAt(), p.closedAt(), p.closeReason(),
                ExitReplayEngine.netPnl(p.entryPrice(), p.exitPrice(), p.volume(), s.fee()),
                pctOf(ExitReplayEngine.netPnl(p.entryPrice(), p.exitPrice(), p.volume(), s.fee()), p))).toList();
        ReplayStats actualStats = ReplayStats.of(actual);
        BigDecimal recorded = covered.stream().map(ReplayPosition::recordedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Calibration> calibrations = new ArrayList<>();
        for (PriceModel model : PriceModel.values()) {
            ReplayStats replay = ReplayStats.of(simulateAll(covered, in.series(), ExitReplayConfig.historical(model,
                    s.decisionFee(), s.fee())));
            double pnlGap = gap(replay.total().doubleValue(), actualStats.total().doubleValue());
            double stopGap = gap(stops(replay), stops(actualStats));
            calibrations.add(new Calibration(model, replay,
                    Math.abs(pnlGap) <= GATE_PNL * 100 && Math.abs(stopGap) <= GATE_STOPS * 100, pnlGap, stopGap));
        }

        StringBuilder md = new StringBuilder();
        md.append("# 코인 출구 리플레이 (작업 6-A) — ").append(s.reportDate()).append("\n\n");
        md.append("> `./gradlew replay -Dreplay.db=<DB 사본>` 이 생성한 보고서 (ADR trading/observability/0005). 운영 DB 사본을 읽기 전용으로")
                .append(" 열고, 1분봉은 빗썸 공개 API(캐시 `").append(RegimeReplayRunner.slash(s.cache())).append("`)에서 받는다.\n\n");
        md.append("## 입력\n\n");
        md.append(Markdown.table(List.of("항목", "값"), List.of(
                List.of("DB 사본", in.dbLabel()),
                List.of("DB 시각 - KST", in.zoneLabel()),
                List.of("대상", s.market() + " 청산 포지션, 진입 " + s.exitFrom() + " 이후 " + in.positions().size()
                        + "건 (PAPER 판별 제외 " + in.excluded() + "건 — 2026-07-12 이전 신호 매수는 체결 연결이 없어 판별 불가)"),
                List.of("1분봉 범위 밖", (in.positions().size() - covered.size()) + "건 (리플레이에서 뺌)"),
                List.of("신호", in.signalCount() + "행 (신호 매도·SIGNAL_GATED 판정에 사용)"),
                List.of("수수료", "손익 " + s.fee() * 100 + "% 양쪽(실측) · 판정 " + s.decisionFee() * 100
                        + "%(운영 taker-fee-rate — 봇이 청산 수수료를 이 값으로 추정했다)"))));
        md.append("\n## 보정 게이트 — 현행 설정이 실제를 재현하는가\n\n");
        md.append("현행 = 손절 -1.5% · TP +3% · 트레일링 0.8%(활성화 +1.5%) · maxHold 360분 · BREAKEVEN_CLOSE · 틱 하한 없음. ")
                .append("통과 기준: 총손익 ±").append((int) (GATE_PNL * 100)).append("%, 손절 건수 ±")
                .append((int) (GATE_STOPS * 100)).append("% (실제는 같은 포지션의 실제 청산가를 모델 수수료로 다시 계산).\n\n");
        List<List<String>> calRows = new ArrayList<>();
        calRows.add(row("실제 (모델 수수료 재계산)", actualStats, "—"));
        for (Calibration c : calibrations) {
            calRows.add(row("리플레이 " + c.model(), c.replay(), String.format("총손익 %+.1f%% · 손절 %+.1f%% → %s",
                    c.pnlGapPct(), c.stopGapPct(), c.passed() ? "**통과**" : "실패")));
        }
        md.append(Markdown.table(List.of("구분", "건수", "총손익(원)", "손절", "승률", "출구 사유", "게이트"), calRows));
        md.append("\nDB 기록 실현손익 합계 ").append(Markdown.won(recorded))
                .append("원 (2026-09-27 이전 체결은 수수료 0 으로 기록돼 모델 수수료 재계산과 다르다). 진단 문서 기준값: 245건 / -8,892원 / 손절 124건.\n");

        List<PriceModel> passed = calibrations.stream().filter(Calibration::passed).map(Calibration::model).toList();
        if (covered.isEmpty()) {
            md.append("\n1분봉 범위 안의 포지션이 없어 리플레이하지 않았다.\n");
            return md.toString();
        }
        if (passed.isEmpty()) {
            md.append("\n**게이트 실패 — 대안 결과를 신뢰할 수 없어 그리드를 돌리지 않았다.** 원인 후보: 봇은 1분마다 한 번 현재가를 읽고 시장가로")
                    .append(" 청산한다(체결 지연·슬리피지 — 실제 손절 평균 -1.77% vs 판정 -1.5%), 신호 쿨다운·포지션 간 상호작용·봇 정지 구간은")
                    .append(" 모델 밖, 2026-09-27 이전 기록 수수료 0. 위 표의 출구 사유 분포 차이로 어느 출구가 어긋나는지 먼저 본다.\n");
            return md.toString();
        }
        for (PriceModel model : passed) {
            appendGrid(md, model, covered, in.series(), s);
        }
        md.append("\n## 과최적화 경고\n\n표본은 한 기간(약 4개월)·").append(covered.size())
                .append("건뿐이고 144개 설정 중 최고를 고르면 우연을 고르기 쉽다. 7~8월로 고른 설정이 9~10월에도 상위인지(위 표)를 먼저 보고,")
                .append(" 기본값 변경은 그 결과와 PAPER 누적을 함께 근거로 한다(ADR trading/strategy/0011 후속).\n");
        return md.toString();
    }

    private static void appendGrid(StringBuilder md, PriceModel model, List<ReplayPosition> covered, MinuteSeries series,
                                   ReplaySettings s) {
        ExitReplayConfig base = ExitReplayConfig.historical(model, s.decisionFee(), s.fee());
        List<Row> rows = new ArrayList<>();
        for (double trailing : TRAILING) {
            for (double activation : ACTIVATION) {
                for (TimeExitMode mode : TimeExitMode.values()) {
                    for (long hold : MAX_HOLD) {
                        for (int ticks : TICKS) {
                            ExitReplayConfig config = base.withExits(trailing, activation, mode, hold, ticks);
                            List<ReplayTrade> trades = simulateAll(covered, series, config);
                            rows.add(new Row(config, ReplayStats.of(trades),
                                    ReplayStats.of(trades.stream().filter(t -> t.openedAt().toLocalDate().isBefore(s.exitSplit())).toList()),
                                    ReplayStats.of(trades.stream().filter(t -> !t.openedAt().toLocalDate().isBefore(s.exitSplit())).toList())));
                        }
                    }
                }
            }
        }
        rows.sort(Comparator.comparing((Row r) -> r.all().total()).reversed());
        md.append("\n## 그리드 — ").append(model).append(" (").append(rows.size()).append("개 설정, 총손익 순)\n\n");
        List<List<String>> table = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            List<String> cells = new ArrayList<>(Markdown.statsRow((i + 1) + ". " + r.config().label(), r.all()));
            table.add(cells);
        }
        md.append(Markdown.table(Markdown.STATS_HEADER, table));

        md.append("\n### 상위 3 + 분할 검증 (").append(s.exitSplit()).append(" 이전 선택 → 이후 검증)\n\n");
        List<Row> byFirst = rows.stream().sorted(Comparator.comparing((Row r) -> r.firstHalf().total()).reversed()).toList();
        List<Row> bySecond = rows.stream().sorted(Comparator.comparing((Row r) -> r.secondHalf().total()).reversed()).toList();
        List<List<String>> split = new ArrayList<>();
        for (Row r : rows.subList(0, Math.min(3, rows.size()))) {
            split.add(splitRow("전체 상위", r, byFirst, bySecond));
        }
        for (Row r : byFirst.subList(0, Math.min(3, byFirst.size()))) {
            split.add(splitRow("앞 구간 상위", r, byFirst, bySecond));
        }
        md.append(Markdown.table(List.of("구분", "설정", "전체 총손익", "앞 구간 총손익(순위)", "뒤 구간 총손익(순위)"), split));
    }

    private static List<String> splitRow(String kind, Row r, List<Row> byFirst, List<Row> bySecond) {
        return List.of(kind, r.config().label(), Markdown.won(r.all().total()),
                Markdown.won(r.firstHalf().total()) + " (" + (byFirst.indexOf(r) + 1) + ")",
                Markdown.won(r.secondHalf().total()) + " (" + (bySecond.indexOf(r) + 1) + ")");
    }

    static List<ReplayTrade> simulateAll(List<ReplayPosition> positions, MinuteSeries series, ExitReplayConfig config) {
        List<ReplayTrade> trades = new ArrayList<>();
        for (ReplayPosition p : positions) {
            ExitOutcome out = ExitReplayEngine.simulate(p, series, config);
            if (out != null) {
                trades.add(new ReplayTrade(p.openedAt(), out.exitTime(), out.reason(), out.pnl(), out.pnlPct()));
            }
        }
        return trades;
    }

    private static List<String> row(String label, ReplayStats st, String gate) {
        return List.of(label, String.valueOf(st.count()), Markdown.won(st.total()), String.valueOf(stops(st)),
                Markdown.plainPct(st.winRatePct()), Markdown.reasons(st.reasons()), gate);
    }

    private static int stops(ReplayStats st) {
        return st.reasons().getOrDefault("STOP_LOSS", 0);
    }

    private static double gap(double replay, double actual) {
        if (actual == 0) {
            return replay == 0 ? 0 : Double.POSITIVE_INFINITY;
        }
        return (replay - actual) / Math.abs(actual) * 100;
    }

    private static double pctOf(BigDecimal pnl, ReplayPosition p) {
        return pnl.doubleValue() / p.entryPrice().multiply(p.volume()).doubleValue() * 100;
    }
}
