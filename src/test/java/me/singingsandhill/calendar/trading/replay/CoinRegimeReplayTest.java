package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * 작업 6-B·6-C 실행 진입점 — 기본 test 에서 제외(@Tag replay), {@code ./gradlew replay} 로 돈다. 6-C 는 공개 API 일봉만으로,
 * 6-B 는 -Dreplay.db 가 있을 때만. 일봉을 받지 못하면(네트워크) 그 사실만 적고 6-B·6-C 를 건너뛴다.
 * 보고서: {@code <replay.out>/coin-regime-replay-<날짜>.md}.
 */
@Tag("replay")
class CoinRegimeReplayTest {

    /** 레짐 그리드의 가장 긴 MA(60일) + 히스테리시스·최소 유지가 자리 잡을 여유. */
    private static final int WARM_UP_DAYS = 150;

    @Test
    void regimeAndRebalanceReplay() throws Exception {
        ReplaySettings s = ReplaySettings.fromSystemProperties();
        Files.createDirectories(s.out());
        Path file = s.out().resolve("coin-regime-replay-" + s.reportDate() + ".md");
        List<DailyBar> daily;
        try {
            daily = BithumbPublicCandles.live(s.cache(), s.market(), LocalDate.now(ReplaySettings.KST))
                    .dailyBars(s.dailyFrom().minusDays(WARM_UP_DAYS));
        } catch (IOException e) {
            Files.writeString(file, "# 코인 레짐 리플레이 (작업 6-B·6-C) — " + s.reportDate() + "\n\n빗썸 공개 일봉을 받지 못해 6-B·6-C 를"
                    + " 건너뛰었다: " + e.getMessage() + "\n");
            System.out.println("[replay] daily candles unavailable — wrote " + file.toAbsolutePath());
            return;
        }
        RegimeReplayRunner.Input input;
        if (s.db() == null) {
            input = new RegimeReplayRunner.Input(daily, null, 0, null, null, s);
        } else {
            try (ReplayDb db = ReplayDb.open(s.db(), s.dbUser(), s.dbPassword(), s.dbZone())) {
                input = new RegimeReplayRunner.Input(daily, db.closedPositions(s.market(), s.regimeFrom().atStartOfDay()),
                        db.countExcluded(s.market(), s.regimeFrom().atStartOfDay()), s.db().getFileName().toString(),
                        CoinExitReplayTest.zoneLabel(db, s), s);
            }
        }
        Files.writeString(file, RegimeReplayRunner.report(input));
        System.out.println("[replay] wrote " + file.toAbsolutePath());
    }
}
