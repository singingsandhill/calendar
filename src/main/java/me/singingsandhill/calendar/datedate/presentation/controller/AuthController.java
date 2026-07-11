package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class AuthController {

    private final SeoService seoService;

    public AuthController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        if (AuthenticatedUsers.currentUserId(authentication).isPresent()) {
            return "redirect:/me";
        }
        model.addAttribute("seo", seoService.getLoginSeo());
        return "auth/login";
    }
}
