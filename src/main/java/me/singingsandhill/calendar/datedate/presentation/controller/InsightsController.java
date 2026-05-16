package me.singingsandhill.calendar.datedate.presentation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.dto.PopularItemDto;
import me.singingsandhill.calendar.datedate.application.dto.ServiceStatsDto;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;

import java.util.List;

@Controller
@RequestMapping("/insights")
public class InsightsController {

    private static final int TOP_ITEMS_LIMIT = 10;

    private final InsightsService insightsService;
    private final PopularityService popularityService;
    private final SeoService seoService;
    private final LocaleLinks localeLinks;

    public InsightsController(InsightsService insightsService,
                              PopularityService popularityService,
                              SeoService seoService,
                              LocaleLinks localeLinks) {
        this.insightsService = insightsService;
        this.popularityService = popularityService;
        this.seoService = seoService;
        this.localeLinks = localeLinks;
    }

    @GetMapping
    public ResponseEntity<Void> insightsRoot() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, localeLinks.href("/insights/trends"))
                .build();
    }

    @GetMapping("/trends")
    public String trends(Model model) {
        List<PopularItemDto> popularLocations = popularityService.getPopularLocations(TOP_ITEMS_LIMIT);
        List<PopularItemDto> popularMenus = popularityService.getPopularMenus(TOP_ITEMS_LIMIT);
        ServiceStatsDto stats = insightsService.getServiceStats();

        // 빈 데이터 환경에서는 thin-page 신호 회피를 위해 noindex + 광고 OFF 로 강등.
        // (PP-Full "콘텐츠가 거의 없는 화면" 가드 — SEO 의 hasData 분기가 메타·광고 동시 처리)
        boolean hasData = !popularLocations.isEmpty()
                || !popularMenus.isEmpty()
                || stats.totalSchedules() > 0;

        model.addAttribute("seo", seoService.getInsightsTrendsSeo(hasData));
        model.addAttribute("popularLocations", popularLocations);
        model.addAttribute("popularMenus", popularMenus);
        model.addAttribute("stats", stats);
        return "insights/trends";
    }
}
