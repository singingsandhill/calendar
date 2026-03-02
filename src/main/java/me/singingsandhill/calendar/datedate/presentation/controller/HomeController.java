package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;

@Controller
public class HomeController {

    private final OwnerService ownerService;
    private final SeoService seoService;
    private final PopularityService popularityService;
    private final InsightsService insightsService;

    public HomeController(OwnerService ownerService,
                          SeoService seoService,
                          PopularityService popularityService,
                          InsightsService insightsService) {
        this.ownerService = ownerService;
        this.seoService = seoService;
        this.popularityService = popularityService;
        this.insightsService = insightsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("seo", seoService.getHomeSeo());
        model.addAttribute("popularLocations", popularityService.getPopularLocations());
        model.addAttribute("popularMenus", popularityService.getPopularMenus());
        model.addAttribute("overview", insightsService.getInsightsOverview());
        return "index";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId) {
        String normalizedId = ownerId.toLowerCase();
        ownerService.getOrCreateOwner(normalizedId);
        return "redirect:/" + normalizedId;
    }
}
