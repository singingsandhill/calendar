package me.singingsandhill.calendar.trading.presentation.controller;

import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.application.service.TradingBotService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/trading")
public class TradingDashboardController {

    private final TradingBotService tradingBotService;
    private final ProfitService profitService;
    private final TradingProperties tradingProperties;

    public TradingDashboardController(TradingBotService tradingBotService,
                                       ProfitService profitService,
                                       TradingProperties tradingProperties) {
        this.tradingBotService = tradingBotService;
        this.profitService = profitService;
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
}
