package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 보고서 흐름 — 보정 게이트를 통과해야만 그리드를 돌리고(작업 6 정의), 6-B 는 DB 사본이 있을 때만, 6-C 는 일봉만으로.
 * 평탄 봉이라 HIGH_LOW·CLOSE 두 모델이 같은 출구를 낸다.
 */
class ReplayReportTest {

    private static final ReplaySettings SETTINGS = new ReplaySettings(null, "sa", "", null, "KRW-ADA",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 1, 4), LocalDate.of(2026, 6, 5),
            LocalDate.of(2024, 10, 1), 0.0004, 0.0025, Path.of("out"), Path.of("build/replay"), LocalDate.of(2026, 10, 22));

    private static ReplayBar flat(LocalDateTime t, String price) {
        return ReplayBar.flat(t, new BigDecimal(price));
    }

    private static ReplayPosition position(long id, LocalDateTime opened, LocalDateTime closed, String exit, String reason) {
        return new ReplayPosition(id, opened, closed, new BigDecimal("290"), BigDecimal.TEN, BigDecimal.ZERO,
                new BigDecimal(exit), reason, BigDecimal.ZERO);
    }

    /** 손절(285, 10:02)과 익절(300, 11:01) 두 건. */
    private static MinuteSeries series() {
        LocalDateTime d = LocalDateTime.of(2026, 8, 1, 10, 0);
        return new MinuteSeries(List.of(flat(d, "290"), flat(d.plusMinutes(1), "288"), flat(d.plusMinutes(2), "285"),
                flat(d.plusMinutes(60), "290"), flat(d.plusMinutes(61), "300"), flat(d.plusMinutes(90), "300")), List.of());
    }

    @Test
    void gridRunsOnlyForModelsThatReproduceTheActualExits() {
        LocalDateTime d = LocalDateTime.of(2026, 8, 1, 10, 0, 30);
        List<ReplayPosition> positions = List.of(position(1, d, d.plusMinutes(2), "285", "STOP_LOSS"),
                position(2, d.plusMinutes(60), d.plusMinutes(61), "300", "TAKE_PROFIT"));

        String report = ExitReplayRunner.report(new ExitReplayRunner.Input(positions, 0, series(), 0, "copy", "-9h", SETTINGS));

        assertThat(report).contains("총손익 +0.0% · 손절 +0.0% → **통과**");
        assertThat(report).contains("## 그리드 — HIGH_LOW (144개 설정").contains("## 그리드 — CLOSE (144개 설정");
        assertThat(report).contains("### 상위 3 + 분할 검증");
    }

    @Test
    void aFailedGateStopsBeforeTheGrid() {
        LocalDateTime d = LocalDateTime.of(2026, 8, 1, 10, 0, 30);
        // 실제는 250 에서 손절됐다고 기록 — 리플레이(285)와 총손익이 크게 어긋난다
        List<ReplayPosition> positions = List.of(position(1, d, d.plusMinutes(2), "250", "STOP_LOSS"),
                position(2, d.plusMinutes(60), d.plusMinutes(61), "300", "TAKE_PROFIT"));

        String report = ExitReplayRunner.report(new ExitReplayRunner.Input(positions, 0, series(), 0, "copy", "-9h", SETTINGS));

        assertThat(report).contains("**게이트 실패").doesNotContain("## 그리드");
    }

    @Test
    void regimeReportSkipsTheFilterEvaluationWithoutADbCopy() {
        List<DailyBar> bars = new ArrayList<>();
        LocalDate start = LocalDate.of(2024, 6, 1);
        for (int i = 0; i < 300; i++) {
            BigDecimal close = BigDecimal.valueOf(500 + Math.round(100 * Math.sin(i / 20.0)));
            bars.add(new DailyBar(start.plusDays(i), close, close.add(BigDecimal.TEN), close.subtract(BigDecimal.TEN), close));
        }

        String report = RegimeReplayRunner.report(new RegimeReplayRunner.Input(bars, null, 0, null, null, SETTINGS));

        assertThat(report).contains("운영 DB 사본이 없어 건너뛰었다");
        assertThat(report).contains("| 단순 보유 |").contains("| 엔진 ma 60 · h 5% · min 3 |");
        assertThat(report).contains("### 판정 (4-D: KRW 보유·단순 보유");
    }

    @Test
    void regimeReportSplitsTheFilterEvaluationIntoTwoPeriods() {
        List<DailyBar> bars = new ArrayList<>();
        LocalDate start = LocalDate.of(2025, 10, 1);
        for (int i = 0; i < 300; i++) {
            BigDecimal close = BigDecimal.valueOf(300 + Math.round(50 * Math.sin(i / 15.0)));
            bars.add(new DailyBar(start.plusDays(i), close, close, close, close));
        }
        List<ReplayPosition> positions = List.of(
                position(1, LocalDateTime.of(2026, 2, 1, 10, 0), LocalDateTime.of(2026, 2, 1, 12, 0), "295", "SIGNAL"),
                position(2, LocalDateTime.of(2026, 7, 1, 10, 0), LocalDateTime.of(2026, 7, 1, 12, 0), "285", "STOP_LOSS"));
        ReplaySettings settings = new ReplaySettings(null, "sa", "", null, "KRW-ADA", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 1, 4), LocalDate.of(2026, 6, 5), LocalDate.of(2025, 12, 1),
                0.0004, 0.0025, Path.of("out"), Path.of("build/replay"), LocalDate.of(2026, 10, 22));

        String report = RegimeReplayRunner.report(new RegimeReplayRunner.Input(bars, positions, 3, "copy", "-9h", settings));

        assertThat(report).contains("### 구간 1 (1건)").contains("### 구간 2 (1건)").contains("PAPER 판별 제외 3건");
        assertThat(report).contains("### 판정 (4-D: 두 구간 모두");
    }
}
