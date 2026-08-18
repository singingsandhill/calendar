package me.singingsandhill.calendar.trading.presentation.controller;

import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.application.service.TradingAnalyticsService;
import me.singingsandhill.calendar.trading.application.service.TradingBotService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/trading")
public class TradingDashboardController {

    private final TradingBotService tradingBotService;
    private final ProfitService profitService;
    private final TradingAnalyticsService analyticsService;
    private final TradingProperties tradingProperties;

    public TradingDashboardController(TradingBotService tradingBotService,
                                       ProfitService profitService,
                                       TradingAnalyticsService analyticsService,
                                       TradingProperties tradingProperties) {
        this.tradingBotService = tradingBotService;
        this.profitService = profitService;
        this.analyticsService = analyticsService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 메인 대시보드
     */
    @GetMapping
    public String dashboard(Model model) {
        TradingBotService.BotStatus botStatus = tradingBotService.getStatus();
        ProfitService.ProfitSummary profitSummary = profitService.getProfitSummary();

        model.addAttribute("botStatus", botStatus);
        model.addAttribute("profitSummary", profitSummary);
        model.addAttribute("market", tradingProperties.getBot().getMarket());

        return "trading/dashboard";
    }

    /**
     * 거래 내역 페이지
     */
    @GetMapping("/trades")
    public String trades(Model model) {
        model.addAttribute("market", tradingProperties.getBot().getMarket());
        return "trading/trades";
    }

    /**
     * 설정 페이지
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("tradingProperties", tradingProperties);
        return "trading/settings";
    }

    /**
     * 포트폴리오 / 리밸런싱 페이지
     */
    @GetMapping("/portfolio")
    public String portfolio(Model model) {
        model.addAttribute("market", tradingProperties.getBot().getMarket());
        model.addAttribute("rebalancing", tradingProperties.getRebalancing());
        return "trading/portfolio";
    }

    /**
     * 검증 페이지
     */
    @GetMapping("/verify")
    public String verify(Model model) {
        model.addAttribute("market", tradingProperties.getBot().getMarket());
        return "trading/verify";
    }

    /**
     * 신호 품질 분석 페이지 (ADR trading/observability/0001).
     *
     * <p>스케줄러 없이 요청 시점에 계산한다 — 페이지는 보낼 것이 없어 주기 갱신 개념이 없고,
     * 신호·포지션이 영구 보관이라 과거 어느 구간이든 다시 계산하면 된다.
     */
    @GetMapping("/analytics")
    public String analytics(@RequestParam(name = "days", defaultValue = "30") int days, Model model) {
        model.addAttribute("report", analyticsService.analyze(days));
        model.addAttribute("market", tradingProperties.getBot().getMarket());
        model.addAttribute("thresholds", tradingProperties.getThresholds());
        model.addAttribute("windowOptions", List.of(7, 14, 30, 60, 90));
        // 서비스가 클램프한 값을 그대로 돌려줘, ?days=9999 로 들어와도 선택 상자가 실제 구간을 가리킨다.
        model.addAttribute("selectedDays", TradingAnalyticsService.clampDays(days));
        return "trading/analytics";
    }
}
