package me.singingsandhill.calendar.trading.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.singingsandhill.calendar.trading.domain.regime.DailyBar;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * 빗썸 공개 캔들 (KST) + 파일 캐시. 1분봉은 KST 날짜별 CSV(지난 날만 캐시), 일봉은 한 파일({@code <코인>-daily.csv}).
 * 공개 API 는 2026-10-22 실측 기준 1년 전 1분봉도 준다(빈 분은 건너뜀). to 는 그 시각을 포함하지 않으므로 페이지의 가장 이른
 * 봉 시각을 다음 to 로 쓴다. 형성 중인 오늘 봉은 버린다.
 */
final class BithumbPublicCandles {

    /** GET 본문을 돌려준다 — 테스트는 가짜로 바꾼다. */
    interface Http {
        String get(String url) throws IOException, InterruptedException;
    }

    private static final String BASE = "https://api.bithumb.com";
    private static final DateTimeFormatter TO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Http http;
    private final Path cacheDir;
    private final String market;
    private final LocalDate todayKst;
    private final long pauseMillis;

    BithumbPublicCandles(Http http, Path cacheDir, String market, LocalDate todayKst, long pauseMillis) {
        this.http = http;
        this.cacheDir = cacheDir;
        this.market = market;
        this.todayKst = todayKst;
        this.pauseMillis = pauseMillis;
    }

    static BithumbPublicCandles live(Path cacheDir, String market, LocalDate todayKst) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        Http http = url -> {
            for (int attempt = 1; ; attempt++) {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status == 200) {
                    return response.body();
                }
                if ((status == 429 || status >= 500) && attempt < 5) {
                    Thread.sleep(1000L * attempt);
                    continue;
                }
                String body = response.body();
                throw new IOException("HTTP " + status + " " + url + " — " + body.substring(0, Math.min(200, body.length())));
            }
        };
        return new BithumbPublicCandles(http, cacheDir, market, todayKst, 120);
    }

    /** from ~ toInclusive (KST 날짜, 오늘은 제외) 1분봉, 시각 오름차순. */
    List<ReplayBar> minuteBars(LocalDate from, LocalDate toInclusive) throws IOException, InterruptedException {
        List<ReplayBar> bars = new ArrayList<>();
        LocalDate last = toInclusive.isBefore(todayKst) ? toInclusive : todayKst.minusDays(1);
        for (LocalDate day = from; !day.isAfter(last); day = day.plusDays(1)) {
            Path file = cacheDir.resolve("candles-1m").resolve(market).resolve(day + ".csv");
            List<ReplayBar> dayBars;
            if (Files.exists(file)) {
                dayBars = readMinutes(file);
            } else {
                dayBars = fetchMinuteDay(day);
                writeMinutes(file, dayBars);
            }
            bars.addAll(dayBars);
        }
        return bars;
    }

    /** from 이후 확정 일봉(오늘 제외), 날짜 오름차순. 캐시가 [from, 어제] 를 덮지 못하면 다시 받는다. */
    List<DailyBar> dailyBars(LocalDate from) throws IOException, InterruptedException {
        Path file = cacheDir.resolve(market.substring(market.indexOf('-') + 1).toLowerCase(Locale.ROOT) + "-daily.csv");
        if (Files.exists(file)) {
            List<DailyBar> cached = readDays(file);
            if (!cached.isEmpty() && !cached.get(0).date().isAfter(from)
                    && !cached.get(cached.size() - 1).date().isBefore(todayKst.minusDays(1))) {
                return cached.stream().filter(b -> !b.date().isBefore(from) && b.date().isBefore(todayKst)).toList();
            }
        }
        TreeMap<LocalDate, DailyBar> days = new TreeMap<>();
        LocalDateTime to = todayKst.atStartOfDay();
        while (true) {
            List<DailyBar> page = parseDays(http.get(BASE + "/v1/candles/days?market=" + market + "&to=" + TO.format(to)
                    + "&count=200"));
            if (page.isEmpty()) {
                break;
            }
            LocalDate earliest = page.stream().map(DailyBar::date).min(LocalDate::compareTo).orElseThrow();
            page.stream().filter(b -> b.date().isBefore(todayKst)).forEach(b -> days.put(b.date(), b));
            if (!earliest.isAfter(from) || !earliest.atStartOfDay().isBefore(to)) {
                break;
            }
            to = earliest.atStartOfDay();
            pause();
        }
        List<DailyBar> bars = new ArrayList<>(days.values());
        writeDays(file, bars);
        return bars.stream().filter(b -> !b.date().isBefore(from)).toList();
    }

    private List<ReplayBar> fetchMinuteDay(LocalDate day) throws IOException, InterruptedException {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        TreeMap<LocalDateTime, ReplayBar> bars = new TreeMap<>();
        LocalDateTime to = end;
        while (true) {
            List<ReplayBar> page = parseMinutes(http.get(BASE + "/v1/candles/minutes/1?market=" + market + "&to="
                    + TO.format(to) + "&count=200"));
            if (page.isEmpty()) {
                break;
            }
            LocalDateTime earliest = page.stream().map(ReplayBar::time).min(LocalDateTime::compareTo).orElseThrow();
            page.stream().filter(b -> !b.time().isBefore(start) && b.time().isBefore(end)).forEach(b -> bars.put(b.time(), b));
            if (!earliest.isAfter(start) || !earliest.isBefore(to)) {
                break;
            }
            to = earliest;
            pause();
        }
        return new ArrayList<>(bars.values());
    }

    static List<ReplayBar> parseMinutes(String body) throws IOException {
        List<ReplayBar> bars = new ArrayList<>();
        for (JsonNode n : array(body)) {
            bars.add(new ReplayBar(LocalDateTime.parse(n.get("candle_date_time_kst").asText()), decimal(n, "opening_price"),
                    decimal(n, "high_price"), decimal(n, "low_price"), decimal(n, "trade_price")));
        }
        return bars;
    }

    static List<DailyBar> parseDays(String body) throws IOException {
        List<DailyBar> bars = new ArrayList<>();
        for (JsonNode n : array(body)) {
            bars.add(new DailyBar(LocalDateTime.parse(n.get("candle_date_time_kst").asText()).toLocalDate(),
                    decimal(n, "opening_price"), decimal(n, "high_price"), decimal(n, "low_price"), decimal(n, "trade_price")));
        }
        return bars;
    }

    private static JsonNode array(String body) throws IOException {
        JsonNode root = JSON.readTree(body);
        if (!root.isArray()) {
            throw new IOException("캔들 응답이 배열이 아니다: " + body.substring(0, Math.min(200, body.length())));
        }
        return root;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return new BigDecimal(node.get(field).asText());
    }

    private void pause() throws InterruptedException {
        if (pauseMillis > 0) {
            Thread.sleep(pauseMillis);
        }
    }

    private static List<ReplayBar> readMinutes(Path file) throws IOException {
        List<ReplayBar> bars = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("time")) {
                continue;
            }
            String[] f = line.split(",");
            bars.add(new ReplayBar(LocalDateTime.parse(f[0]), new BigDecimal(f[1]), new BigDecimal(f[2]),
                    new BigDecimal(f[3]), new BigDecimal(f[4])));
        }
        return bars;
    }

    private static void writeMinutes(Path file, List<ReplayBar> bars) throws IOException {
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>(List.of("time_kst,open,high,low,close"));
        for (ReplayBar b : bars) {
            lines.add(b.time() + "," + b.open().toPlainString() + "," + b.high().toPlainString() + ","
                    + b.low().toPlainString() + "," + b.close().toPlainString());
        }
        Files.write(file, lines);
    }

    private static List<DailyBar> readDays(Path file) throws IOException {
        List<DailyBar> bars = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("date")) {
                continue;
            }
            String[] f = line.split(",");
            bars.add(new DailyBar(LocalDate.parse(f[0]), new BigDecimal(f[1]), new BigDecimal(f[2]), new BigDecimal(f[3]),
                    new BigDecimal(f[4])));
        }
        return bars;
    }

    private static void writeDays(Path file, List<DailyBar> bars) throws IOException {
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>(List.of("date_kst,open,high,low,close"));
        for (DailyBar b : bars) {
            lines.add(b.date() + "," + b.open().toPlainString() + "," + b.high().toPlainString() + ","
                    + b.low().toPlainString() + "," + b.close().toPlainString());
        }
        Files.write(file, lines);
    }
}
