package me.singingsandhill.calendar.trading.replay;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 작업 6-A 실행 진입점 — 기본 test 에서 제외(@Tag replay), {@code ./gradlew replay -Dreplay.db=<scheduledb 사본>} 로만 돈다.
 * 보고서: {@code <replay.out>/coin-exit-replay-<날짜>.md}. 운영 DB 에는 접속하지 않는다.
 */
@Tag("replay")
class CoinExitReplayTest {

    @Test
    void exitReplay() throws Exception {
        ReplaySettings s = ReplaySettings.fromSystemProperties();
        assumeTrue(s.db() != null, "replay.db 미지정 — 6-A 는 운영 DB 사본이 필요하다: ./gradlew replay -Dreplay.db=<경로>");
        try (ReplayDb db = ReplayDb.open(s.db(), s.dbUser(), s.dbPassword(), s.dbZone())) {
            LocalDateTime from = s.exitFrom().atStartOfDay();
            List<ReplayPosition> positions = db.closedPositions(s.market(), from);
            assumeTrue(!positions.isEmpty(), "대상 포지션 없음 (" + s.market() + ", 진입 " + s.exitFrom() + " 이후)");
            LocalDate end = positions.stream().map(p -> p.closedAt().toLocalDate()).max(LocalDate::compareTo).orElseThrow()
                    .plusDays(3);
            List<ReplayBar> bars = BithumbPublicCandles.live(s.cache(), s.market(), LocalDate.now(ReplaySettings.KST))
                    .minuteBars(s.exitFrom(), end);
            List<ReplaySignal> signals = db.signals(s.market(), from, end.plusDays(1).atStartOfDay());
            String report = ExitReplayRunner.report(new ExitReplayRunner.Input(positions,
                    db.countExcluded(s.market(), from), new MinuteSeries(bars, signals), signals.size(),
                    s.db().getFileName().toString(), zoneLabel(db, s), s));
            Files.createDirectories(s.out());
            Path file = s.out().resolve("coin-exit-replay-" + s.reportDate() + ".md");
            Files.writeString(file, report);
            System.out.println("[replay] wrote " + file.toAbsolutePath());
        }
    }

    static String zoneLabel(ReplayDb db, ReplaySettings s) {
        long hours = db.dbMinusKst().toHours();
        return String.format("%+dh", hours) + (s.dbZone() != null ? " (replay.db-zone=" + s.dbZone() + ")"
                : " (자동 추정: 1분봉 created_at - candle_date_time 중앙값)");
    }
}
