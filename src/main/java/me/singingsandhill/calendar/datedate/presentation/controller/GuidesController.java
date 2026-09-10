package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.datedate.application.exception.GuideNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.guide.GuideSlug;
import me.singingsandhill.calendar.datedate.domain.guide.GuideSlugs;

/**
 * /guides 모임 노하우 허브 — 허브 인덱스 + 슬러그별 기사.
 *
 * <p>기사는 슬러그마다 전용 템플릿({@code guides/<slug>.html})을 쓴다 — 비교표·체크리스트
 * 등 기사별 골격이 달라 공용 템플릿의 복제 페이지 신호를 피한다 (ADR common/seo/0011).
 */
@Controller
@RequestMapping("/guides")
public class GuidesController {

    private final SeoService seoService;

    public GuidesController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("seo", seoService.getGuidesIndexSeo());
        model.addAttribute("allGuides", GuideSlugs.ALL);
        return "guides/index";
    }

    @GetMapping("/{slug}")
    public String article(@PathVariable String slug, Model model) {
        // 미지 슬러그는 소프트 404(리다이렉트)가 아니라 HTTP 404 (ADR datedate/domain/0008)
        GuideSlug article = GuideSlugs.find(slug)
                .orElseThrow(() -> new GuideNotFoundException(slug));

        model.addAttribute("seo", seoService.getGuideArticleSeo(slug));
        model.addAttribute("article", article);
        model.addAttribute("allGuides", GuideSlugs.ALL);
        return "guides/" + slug;
    }
}
