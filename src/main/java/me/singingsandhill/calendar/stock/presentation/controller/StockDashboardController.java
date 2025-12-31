package me.singingsandhill.calendar.stock.presentation.controller;

import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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
     * 거래 내역 페이지
     */
    @GetMapping("/history")
    public String history(Model model) {
        LocalDate today = LocalDate.now(KST);
        List<StockPosition> allPositions = positionService.getAllPositions(today);

        model.addAttribute("positions", allPositions);
        model.addAttribute("tradingDate", today);

        return "stock/history";
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
