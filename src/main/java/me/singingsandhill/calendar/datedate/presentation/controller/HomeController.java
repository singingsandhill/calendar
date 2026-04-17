package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("seo", seoService.getGuideSeo());
        return "guide";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("seo", seoService.getPrivacySeo());
        return "privacy";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("seo", seoService.getTermsSeo());
        return "terms";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("seo", seoService.getFaqSeo());
        return "faq";
    }

    @GetMapping("/tools/date-diff")
    public String dateDiff(Model model) {
        model.addAttribute("seo", seoService.getDateDiffSeo());
        return "tools/date-diff";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId, RedirectAttributes redirectAttributes) {
        try {
            String normalizedId = ownerId.toLowerCase();
            ownerService.getOrCreateOwner(normalizedId);
            return "redirect:/" + normalizedId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "페이지를 만들 수 없습니다. 다시 시도해 주세요.");
            return "redirect:/";
        }
    }

}
