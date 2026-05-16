package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

@Controller
@RequestMapping("/use-cases")
public class UseCaseController {

    private final SeoService seoService;
    private final LocaleLinks localeLinks;

    public UseCaseController(SeoService seoService, LocaleLinks localeLinks) {
        this.seoService = seoService;
        this.localeLinks = localeLinks;
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        if (!UseCaseSlugs.ALL.contains(slug)) {
            return localeLinks.redirect("/");
        }

        model.addAttribute("seo", seoService.getUseCaseSeo(slug));
        model.addAttribute("currentSlug", slug);
        model.addAttribute("allSlugs", UseCaseSlugs.ALL);
        return "use-cases/detail";
    }
}
