package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.ReservedOwnerIds;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@Controller
public class HomeController {

    private final OwnerService ownerService;
    private final SeoService seoService;
    private final PopularityService popularityService;
    private final InsightsService insightsService;
    private final MessageSource messageSource;
    private final LocaleLinks localeLinks;

    public HomeController(OwnerService ownerService,
                          SeoService seoService,
                          PopularityService popularityService,
                          InsightsService insightsService,
                          MessageSource messageSource,
                          LocaleLinks localeLinks) {
        this.ownerService = ownerService;
        this.seoService = seoService;
        this.popularityService = popularityService;
        this.insightsService = insightsService;
        this.messageSource = messageSource;
        this.localeLinks = localeLinks;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("seo", seoService.getHomeSeo());
        model.addAttribute("popularLocations", popularityService.getPopularLocations());
        model.addAttribute("popularMenus", popularityService.getPopularMenus());
        model.addAttribute("overview", insightsService.getInsightsOverview());
        model.addAttribute("reservedOwnerIds", ReservedOwnerIds.RESERVED);
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

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("seo", seoService.getAboutSeo());
        return "about";
    }

    @GetMapping("/tools/date-diff")
    public String dateDiff(Model model) {
        model.addAttribute("seo", seoService.getDateDiffSeo());
        return "tools/date-diff";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            String normalizedId = ownerId.toLowerCase();
            Long userId = AuthenticatedUsers.currentUserId(authentication).orElse(null);
            ownerService.getOrCreateOwner(normalizedId, userId);
            return localeLinks.redirect("/" + normalizedId);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveBusinessMessage(e));
            return localeLinks.redirect("/");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return localeLinks.redirect("/");
        } catch (Exception e) {
            String fallback = messageSource.getMessage(
                    "errors.startFailed",
                    null,
                    "Could not create the page. Please try again.",
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("errorMessage", fallback);
            return localeLinks.redirect("/");
        }
    }

    private String resolveBusinessMessage(BusinessException e) {
        if (e.getMessageKey() == null) {
            return e.getMessage();
        }
        return messageSource.getMessage(
                e.getMessageKey(),
                e.getMessageArgs(),
                e.getMessage(),
                LocaleContextHolder.getLocale());
    }

}
