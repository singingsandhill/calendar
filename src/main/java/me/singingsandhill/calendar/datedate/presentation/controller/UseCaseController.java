package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.datedate.application.exception.UseCaseNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

@Controller
@RequestMapping("/use-cases")
public class UseCaseController {

    private final SeoService seoService;

    public UseCaseController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("seo", seoService.getUseCasesIndexSeo());
        model.addAttribute("allSlugs", UseCaseSlugs.ALL);
        return "use-cases/index";
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        // 미지 슬러그를 홈으로 302 하면 크롤러에 소프트 404 로 읽힌다 — owner 404 와 같은
        // 논리로 HTTP 404 (ADR datedate/domain/0008).
        if (!UseCaseSlugs.ALL.contains(slug)) {
            throw new UseCaseNotFoundException(slug);
        }

        model.addAttribute("seo", seoService.getUseCaseSeo(slug));
        model.addAttribute("currentSlug", slug);
        model.addAttribute("allSlugs", UseCaseSlugs.ALL);
        return "use-cases/detail";
    }
}
