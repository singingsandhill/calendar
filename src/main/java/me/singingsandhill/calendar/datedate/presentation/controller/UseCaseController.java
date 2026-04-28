package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.datedate.application.service.SeoService;

@Controller
@RequestMapping("/use-cases")
public class UseCaseController {

    private static final List<String> SLUGS = List.of(
        "friend-meetup",
        "team-meeting",
        "travel-planning",
        "study-group"
    );

    private final SeoService seoService;

    public UseCaseController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        if (!SLUGS.contains(slug)) {
            return "redirect:/";
        }

        model.addAttribute("seo", seoService.getUseCaseSeo(slug));
        model.addAttribute("currentSlug", slug);
        model.addAttribute("allSlugs", SLUGS);
        return "use-cases/detail";
    }
}
