package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class AuthController {

    private final SeoService seoService;
    private final LocaleLinks localeLinks;

    public AuthController(SeoService seoService, LocaleLinks localeLinks) {
        this.seoService = seoService;
        this.localeLinks = localeLinks;
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        if (AuthenticatedUsers.currentUserId(authentication).isPresent()) {
            return localeLinks.redirect("/me");
        }
        model.addAttribute("seo", seoService.getLoginSeo());
        return "auth/login";
    }
}
