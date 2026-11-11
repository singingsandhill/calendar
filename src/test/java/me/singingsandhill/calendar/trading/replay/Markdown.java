package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 보고서 표 서식. */
final class Markdown {

    private Markdown() {
    }

    static String table(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", header)).append(" |\n");
        sb.append("|").append("---|".repeat(header.size())).append("\n");
        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return sb.toString();
    }

    static String won(BigDecimal value) {
        return String.format("%,d", value.setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    static String pct(double value) {
        return String.format("%+.2f%%", value);
    }

    static String plainPct(double value) {
        return String.format("%.1f%%", value);
    }

    static String reasons(Map<String, Integer> reasons) {
        return reasons.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    static List<String> statsRow(String label, ReplayStats s) {
        return List.of(label, String.valueOf(s.count()), plainPct(s.winRatePct()), pct(s.avgWinPct()), pct(s.avgLossPct()),
                pct(s.expectancyPct()), won(s.total()), won(s.maxDrawdown()), reasons(s.reasons()));
    }

    static final List<String> STATS_HEADER = List.of("설정", "건수", "승률", "평균 이익", "평균 손실", "기대값(건당)",
            "총손익(원)", "MDD(원)", "출구 사유");
}
