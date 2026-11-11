package me.singingsandhill.calendar.trading.replay;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 리플레이 입력 — {@code ./gradlew replay -Dreplay.db=/path/scheduledb} 처럼 -D 로 받는다 (ADR trading/observability/0005).
 * 운영 DB 에 접속하지 않는다: replay.db 는 내려받은 H2 파일 사본이고 읽기 전용으로 연다.
 *
 * <ul>
 *   <li>replay.db / replay.db-user(sa) / replay.db-password(빈 값) / replay.db-zone(DB 시각의 시간대, 기본 자동 추정)</li>
 *   <li>replay.market(KRW-ADA) · replay.exit-from(2026-07-01) · replay.exit-split(2026-09-01, 7~8월 선택 → 9~10월 검증)</li>
 *   <li>replay.regime-from(2026-01-04) · replay.regime-split(2026-06-05) · replay.daily-from(2024-10-01)</li>
 *   <li>replay.fee(0.0004 손익 수수료, 양쪽) · replay.decision-fee(0.0025 판정 수수료 = 운영 taker-fee-rate)</li>
 *   <li>replay.out(docs/data-analysis) · replay.cache(build/replay) · replay.date(보고서 날짜, 기본 오늘 KST)</li>
 * </ul>
 */
record ReplaySettings(Path db, String dbUser, String dbPassword, ZoneId dbZone, String market, LocalDate exitFrom,
                      LocalDate exitSplit, LocalDate regimeFrom, LocalDate regimeSplit, LocalDate dailyFrom, double fee,
                      double decisionFee, Path out, Path cache, LocalDate reportDate) {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    static ReplaySettings fromSystemProperties() {
        String db = System.getProperty("replay.db");
        String zone = System.getProperty("replay.db-zone");
        return new ReplaySettings(
                db == null || db.isBlank() ? null : Path.of(db),
                System.getProperty("replay.db-user", "sa"),
                System.getProperty("replay.db-password", ""),
                zone == null || zone.isBlank() ? null : ZoneId.of(zone),
                System.getProperty("replay.market", "KRW-ADA"),
                date("replay.exit-from", "2026-07-01"),
                date("replay.exit-split", "2026-09-01"),
                date("replay.regime-from", "2026-01-04"),
                date("replay.regime-split", "2026-06-05"),
                date("replay.daily-from", "2024-10-01"),
                Double.parseDouble(System.getProperty("replay.fee", "0.0004")),
                Double.parseDouble(System.getProperty("replay.decision-fee", "0.0025")),
                Path.of(System.getProperty("replay.out", "docs/data-analysis")),
                Path.of(System.getProperty("replay.cache", "build/replay")),
                LocalDate.parse(System.getProperty("replay.date", LocalDate.now(KST).toString())));
    }

    private static LocalDate date(String key, String fallback) {
        return LocalDate.parse(System.getProperty(key, fallback));
    }
}
