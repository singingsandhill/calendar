package me.singingsandhill.calendar.datedate.presentation.controller;

import java.time.Clock;
import java.time.Year;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.service.RecapService;
import me.singingsandhill.calendar.datedate.application.service.RecapShareService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class RecapController {

    private final RecapService recapService;
    private final RecapShareService recapShareService;
    private final SeoService seoService;
    private final Clock clock;
    private final String baseUrl;

    public RecapController(RecapService recapService,
                           RecapShareService recapShareService,
                           SeoService seoService,
                           Clock clock,
                           @Value("${app.base-url}") String baseUrl) {
        this.recapService = recapService;
        this.recapShareService = recapShareService;
        this.seoService = seoService;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/recap")
    public String currentYearRecap() {
        return "redirect:/recap/" + Year.now(clock).getValue();
    }

    @GetMapping("/recap/{year:\\d{4}}")
    public String recap(@PathVariable int year, Authentication authentication, Model model) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        RecapDto recap = recapService.buildRecap(userId, year);
        model.addAttribute("recap", recap);
        model.addAttribute("seo", seoService.getRecapSeo(year));
        return "recap/recap";
    }

    @PostMapping("/recap/{year:\\d{4}}/share")
    public String createShare(@PathVariable int year, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        RecapShare share = recapShareService.getOrCreateShare(userId, year);
        redirectAttributes.addFlashAttribute("shareUrl",
                baseUrl + "/recap/share/" + share.getToken());
        return "redirect:/recap/" + year;
    }

    @GetMapping("/recap/share/{token}")
    public String sharedRecap(@PathVariable String token, Model model) {
        RecapShare share = recapShareService.getByToken(token);
        RecapDto recap = recapService.buildRecap(share.getUserId(), share.getYear());
        model.addAttribute("recap", recap);
        model.addAttribute("seo", seoService.getRecapShareSeo(recap.nickname(), recap.year()));
        return "recap/share";
    }
}
