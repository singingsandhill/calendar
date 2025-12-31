package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;

@Controller
public class HomeController {

    private final OwnerService ownerService;
    private final SeoService seoService;
    private final PopularityService popularityService;

    public HomeController(OwnerService ownerService,
                          SeoService seoService,
                          PopularityService popularityService) {
        this.ownerService = ownerService;
        this.seoService = seoService;
        this.popularityService = popularityService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("seo", seoService.getHomeSeo());
        model.addAttribute("popularLocations", popularityService.getPopularLocations());
        model.addAttribute("popularMenus", popularityService.getPopularMenus());
        return "index";
    }

    @GetMapping("/start")
    public String startPage(Model model) {
        model.addAttribute("seo", seoService.getStartPageSeo());
        return "start";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId) {
        String normalizedId = ownerId.toLowerCase();
        ownerService.getOrCreateOwner(normalizedId);
        return "redirect:/" + normalizedId;
    }
}
