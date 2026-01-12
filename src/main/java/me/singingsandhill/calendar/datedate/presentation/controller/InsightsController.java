package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;

@Controller
@RequestMapping("/insights")
public class InsightsController {

    private static final int TOP_ITEMS_LIMIT = 10;

    private final InsightsService insightsService;
    private final PopularityService popularityService;
    private final SeoService seoService;

    public InsightsController(InsightsService insightsService,
                              PopularityService popularityService,
                              SeoService seoService) {
        this.insightsService = insightsService;
        this.popularityService = popularityService;
        this.seoService = seoService;
    }

    @GetMapping
    public String hub(Model model) {
        model.addAttribute("seo", seoService.getInsightsHubSeo());
        model.addAttribute("overview", insightsService.getInsightsOverview());
        return "insights/hub";
    }

    @GetMapping("/trends")
    public String trends(Model model) {
        model.addAttribute("seo", seoService.getInsightsTrendsSeo());
        model.addAttribute("popularLocations", popularityService.getPopularLocations(TOP_ITEMS_LIMIT));
        model.addAttribute("popularMenus", popularityService.getPopularMenus(TOP_ITEMS_LIMIT));
        return "insights/trends";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("seo", seoService.getInsightsStatsSeo());
        model.addAttribute("stats", insightsService.getServiceStats());
        return "insights/stats";
    }
}
