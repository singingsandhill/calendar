package me.singingsandhill.calendar.datedate.presentation.controller;

import java.time.Clock;
import java.time.Year;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class MyPageController {

    private final OwnerService ownerService;
    private final SeoService seoService;
    private final Clock clock;

    public MyPageController(OwnerService ownerService, SeoService seoService, Clock clock) {
        this.ownerService = ownerService;
        this.seoService = seoService;
        this.clock = clock;
    }

    @GetMapping("/me")
    public String myPage(Authentication authentication, Model model) {
        Long userId = AuthenticatedUsers.currentUserId(authentication).orElseThrow();
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        model.addAttribute("nickname",
                principal.getAttribute(KakaoOAuth2UserService.ATTR_APP_NICKNAME));
        model.addAttribute("profileImage",
                principal.getAttribute(KakaoOAuth2UserService.ATTR_APP_PROFILE_IMAGE));
        model.addAttribute("owners", ownerService.getOwnersOf(userId));
        model.addAttribute("currentYear", Year.now(clock).getValue());
        model.addAttribute("seo", seoService.getMyPageSeo());
        return "me/mypage";
    }
}
