package me.singingsandhill.calendar.stock.presentation.controller;

import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 주식 트레이딩 대시보드 컨트롤러
 */
@Controller
@RequestMapping("/stock")
public class StockDashboardController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GapPullbackBotService botService;
    private final ScreeningService screeningService;
    private final StockPositionService positionService;
    private final StockProperties stockProperties;

    public StockDashboardController(GapPullbackBotService botService,
                                     ScreeningService screeningService,
                                     StockPositionService positionService,
                                     StockProperties stockProperties) {
        this.botService = botService;
        this.screeningService = screeningService;
        this.positionService = positionService;
        this.stockProperties = stockProperties;
    }

    /**
     * 메인 대시보드
     */
    @GetMapping
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now(KST);
        GapPullbackBotService.BotStatus botStatus = botService.getStatus();

        // 관심종목 및 포지션 조회
        List<Stock> watchlist = screeningService.getActiveStocks(today);
        List<StockPosition> openPositions = positionService.getOpenPositions(today);
        List<StockPosition> closedPositions = positionService.getClosedPositions(today);

        // P&L 계산
        BigDecimal totalRealizedPnl = closedPositions.stream()
            .map(StockPosition::getRealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long winCount = closedPositions.stream()
            .filter(p -> p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
            .count();

        model.addAttribute("botStatus", botStatus);
        model.addAttribute("watchlist", watchlist);
        model.addAttribute("openPositions", openPositions);
        model.addAttribute("closedPositions", closedPositions);
        model.addAttribute("totalRealizedPnl", totalRealizedPnl);
        model.addAttribute("winCount", winCount);
        model.addAttribute("loseCount", closedPositions.size() - winCount);
        model.addAttribute("tradingDate", today);

        return "stock/dashboard";
    }

    /**
     * 거래 내역 페이지 (필터 + KPI + 분포)
     */
    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false, defaultValue = "time") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false, defaultValue = "month") String preset,
            Model model) {

        LocalDate today = LocalDate.now(KST);

        LocalDate fromDate;
        LocalDate toDate = to != null && !to.isBlank() ? LocalDate.parse(to) : today;
        if (from != null && !from.isBlank()) {
            fromDate = LocalDate.parse(from);
        } else {
            fromDate = switch (preset) {
                case "today" -> today;
                case "week" -> today.minusDays(6);
                case "month" -> today.withDayOfMonth(1);
                default -> today.withDayOfMonth(1);
            };
        }

        List<StockPosition> positions = positionService.getPositionsInRange(fromDate, toDate);

        // 필터 적용
        List<StockPosition> filtered = positions.stream()
            .filter(p -> reason == null || reason.isBlank()
                || (p.getCloseReason() != null && p.getCloseReason().name().equals(reason)))
            .filter(p -> stockCode == null || stockCode.isBlank()
                || p.getStockCode().contains(stockCode))
            .sorted(resolveSort(sort, order))
            .toList();

        // KPI
        List<StockPosition> closed = filtered.stream()
            .filter(p -> p.getRealizedPnl() != null && p.getClosedAt() != null)
            .toList();
        int totalCount = filtered.size();
        int closedCount = closed.size();
        long winCount = closed.stream().filter(p -> p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0).count();
        long loseCount = closed.stream().filter(p -> p.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0).count();
        BigDecimal winRate = closedCount == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(winCount * 100.0 / closedCount).setScale(1, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPnl = closed.stream()
            .map(StockPosition::getRealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPnl = closedCount == 0 ? BigDecimal.ZERO
            : totalPnl.divide(BigDecimal.valueOf(closedCount), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal bestPnl = closed.stream()
            .map(StockPosition::getRealizedPnl)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        BigDecimal worstPnl = closed.stream()
            .map(StockPosition::getRealizedPnl)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        // Close Reason 분포
        Map<String, Long> reasonCounts = new LinkedHashMap<>();
        for (StockCloseReason r : StockCloseReason.values()) {
            long count = closed.stream()
                .filter(p -> p.getCloseReason() == r)
                .count();
            if (count > 0) reasonCounts.put(r.name(), count);
        }

        // 시간대별 승/패 분포 (KST 시간 기준)
        Map<String, int[]> hourBuckets = new LinkedHashMap<>();
        hourBuckets.put("09-10", new int[]{0, 0});
        hourBuckets.put("10-11", new int[]{0, 0});
        hourBuckets.put("11-12", new int[]{0, 0});
        hourBuckets.put("12-15", new int[]{0, 0});
        for (StockPosition p : closed) {
            int hour = p.getEnteredAt() != null ? p.getEnteredAt().getHour() : p.getClosedAt().getHour();
            String bucket = hour < 10 ? "09-10" : hour < 11 ? "10-11" : hour < 12 ? "11-12" : "12-15";
            int[] wl = hourBuckets.get(bucket);
            if (p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0) wl[0]++;
            else wl[1]++;
        }

        // 보유시간 추가용 매핑
        Map<Long, String> heldDurations = new HashMap<>();
        for (StockPosition p : filtered) {
            if (p.getEnteredAt() != null && p.getClosedAt() != null) {
                Duration d = Duration.between(p.getEnteredAt(), p.getClosedAt());
                heldDurations.put(p.getId(), formatDuration(d));
            }
        }

        model.addAttribute("positions", filtered);
        model.addAttribute("heldDurations", heldDurations);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("filterReason", reason == null ? "" : reason);
        model.addAttribute("filterStockCode", stockCode == null ? "" : stockCode);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortOrder", order);
        model.addAttribute("preset", preset);
        model.addAttribute("closeReasons", Arrays.stream(StockCloseReason.values()).map(Enum::name).toList());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("closedCount", closedCount);
        model.addAttribute("winCount", winCount);
        model.addAttribute("loseCount", loseCount);
        model.addAttribute("winRate", winRate);
        model.addAttribute("totalPnl", totalPnl);
        model.addAttribute("avgPnl", avgPnl);
        model.addAttribute("bestPnl", bestPnl);
        model.addAttribute("worstPnl", worstPnl);
        model.addAttribute("reasonCounts", reasonCounts);
        model.addAttribute("reasonMax", reasonCounts.values().stream().max(Long::compareTo).orElse(1L));
        model.addAttribute("hourBuckets", hourBuckets);
        model.addAttribute("tradingDate", today);

        return "stock/history";
    }

    private Comparator<StockPosition> resolveSort(String field, String order) {
        Comparator<StockPosition> comp = switch (field) {
            case "pnl" -> Comparator.comparing(
                p -> p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO);
            case "code" -> Comparator.comparing(StockPosition::getStockCode);
            default -> Comparator.comparing(
                (StockPosition p) -> p.getClosedAt() != null ? p.getClosedAt()
                    : p.getEnteredAt() != null ? p.getEnteredAt()
                    : p.getCreatedAt(),
                Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return "asc".equalsIgnoreCase(order) ? comp : comp.reversed();
    }

    private String formatDuration(Duration d) {
        long totalSec = Math.max(0, d.getSeconds());
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * 설정 페이지
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("stockProperties", stockProperties);
        return "stock/settings";
    }
}
