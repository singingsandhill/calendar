package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** 도구가 하나뿐이라 /tools 인덱스 페이지는 그 자체가 thin — 영구 리다이렉트로 대체 (사이트맵 미등재). */
    @GetMapping("/tools")
    public ResponseEntity<Void> toolsRoot() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, localeLinks.href("/tools/date-diff"))
                .build();
    }

    /**
     * @param generated 값이 "랜덤 생성" 버튼이 준 것인지. 사용자가 직접 입력한 ID 는 이미 있는
     *                  페이지로 돌아오는 재진입이 정상이지만(get-or-create), 방금 뽑은 랜덤 ID 가
     *                  이미 존재한다면 그건 충돌이다 — 남의 페이지로 흘려보내지 않고 실패시킨다.
     *                  위조해도 자기 요청이 에러 날 뿐이라 신뢰 경계는 아니다.
     */
    @PostMapping("/start")
    public String start(@RequestParam String ownerId,
                        @RequestParam(defaultValue = "false") boolean generated,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            String normalizedId = ownerId.toLowerCase();
            Long userId = AuthenticatedUsers.currentUserId(authentication).orElse(null);
            if (generated) {
                ownerService.createOwner(normalizedId, userId);
            } else {
                ownerService.getOrCreateOwner(normalizedId, userId);
            }
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
