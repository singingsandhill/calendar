package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 공개 캔들 페이지 넘김·날짜 경계·캐시 — 가짜 HTTP (to 는 그 시각을 포함하지 않는다). */
class BithumbPublicCandlesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    @TempDir
    Path cache;

    private static String minute(String kst, String price) {
        return "{\"candle_date_time_kst\":\"" + kst + "\",\"opening_price\":" + price + ",\"high_price\":" + price
                + ",\"low_price\":" + price + ",\"trade_price\":" + price + "}";
    }

    private static String day(String date, String close) {
        return "{\"candle_date_time_kst\":\"" + date + "T00:00:00\",\"opening_price\":1,\"high_price\":2,\"low_price\":0.5,"
                + "\"trade_price\":" + close + "}";
    }

    private static BithumbPublicCandles.Http fake(Map<String, String> pages, List<String> calls) {
        return url -> {
            calls.add(url);
            String to = url.substring(url.indexOf("&to=") + 4, url.indexOf("&count="));
            String body = pages.get(to);
            if (body == null) {
                throw new IOException("unexpected " + url);
            }
            return body;
        };
    }

    @Test
    void minuteDayIsPagedBackToMidnightAndCached() throws Exception {
        List<String> calls = new ArrayList<>();
        Map<String, String> pages = Map.of(
                "2026-08-02T00:00:00", "[" + minute("2026-08-01T23:59:00", "291") + "," + minute("2026-08-01T23:58:00", "290") + "]",
                "2026-08-01T23:58:00", "[" + minute("2026-08-01T00:01:00", "289") + "," + minute("2026-08-01T00:00:00", "288")
                        + "," + minute("2026-07-31T23:59:00", "287") + "]");
        BithumbPublicCandles candles = new BithumbPublicCandles(fake(pages, calls), cache, "KRW-ADA", TODAY, 0);

        List<ReplayBar> bars = candles.minuteBars(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1));

        assertThat(bars).extracting(ReplayBar::time).containsExactly(LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 1), LocalDateTime.of(2026, 8, 1, 23, 58), LocalDateTime.of(2026, 8, 1, 23, 59));
        assertThat(calls).hasSize(2);
        assertThat(Files.exists(cache.resolve("candles-1m/KRW-ADA/2026-08-01.csv"))).isTrue();

        // 두 번째는 캐시 — HTTP 호출 없음
        List<ReplayBar> again = new BithumbPublicCandles(url -> {
            throw new IOException("must not fetch");
        }, cache, "KRW-ADA", TODAY, 0).minuteBars(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1));
        assertThat(again).isEqualTo(bars);
    }

    @Test
    void dailyBarsDropTheFormingTodayBarAndPageBack() throws Exception {
        List<String> calls = new ArrayList<>();
        Map<String, String> pages = Map.of(
                "2026-10-22T00:00:00", "[" + day("2026-10-22", "300") + "," + day("2026-10-21", "290") + ","
                        + day("2026-10-20", "280") + "]",
                "2026-10-20T00:00:00", "[" + day("2026-10-19", "270") + "," + day("2026-10-18", "260") + "]");
        BithumbPublicCandles candles = new BithumbPublicCandles(fake(pages, calls), cache, "KRW-ADA", TODAY, 0);

        List<DailyBar> bars = candles.dailyBars(LocalDate.of(2026, 10, 19));

        assertThat(bars).extracting(DailyBar::date).containsExactly(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 21));
        assertThat(Files.readAllLines(cache.resolve("ada-daily.csv"))).hasSize(5);   // 헤더 + 18~21일
    }

    @Test
    void anErrorObjectIsNotTakenForAnEmptyPage() {
        assertThatThrownBy(() -> BithumbPublicCandles.parseMinutes("{\"error\":{\"name\":\"invalid\"}}"))
                .isInstanceOf(IOException.class);
    }
}
