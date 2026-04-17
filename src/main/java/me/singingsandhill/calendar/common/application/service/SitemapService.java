package me.singingsandhill.calendar.common.application.service;

import me.singingsandhill.calendar.common.application.dto.SitemapEntry;
import me.singingsandhill.calendar.runner.domain.Run;
import me.singingsandhill.calendar.runner.domain.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic sitemap.xml generation service.
 * Generates sitemap with real-time lastmod dates based on content updates.
 */
@Service
public class SitemapService {

    // 콘텐츠 최종 수정일 — 실제 페이지 내용이 바뀔 때마다 갱신
    private static final LocalDate HOME_LASTMOD     = LocalDate.of(2026, 4, 16);
    private static final LocalDate GUIDE_LASTMOD    = LocalDate.of(2026, 4, 16);
    private static final LocalDate PRIVACY_LASTMOD  = LocalDate.of(2026, 4, 6);
    private static final LocalDate TERMS_LASTMOD    = LocalDate.of(2026, 4, 6);
    private static final LocalDate USE_CASE_LASTMOD = LocalDate.of(2026, 4, 16);
    private static final LocalDate ANNOUNCE_LASTMOD = LocalDate.of(2026, 4, 6);
    private static final LocalDate FAQ_LASTMOD      = LocalDate.of(2026, 4, 16);
    private static final LocalDate TOOL_LASTMOD     = LocalDate.of(2026, 4, 16);

    private final String baseUrl;
    private final RunRepository runRepository;

    public SitemapService(
            @Value("${app.base-url:https://datedate.site}") String baseUrl,
            @Autowired(required = false) RunRepository runRepository
    ) {
        this.baseUrl = baseUrl;
        this.runRepository = runRepository;
    }

    public List<SitemapEntry> getSitemapEntries() {
        LocalDate runnerLastmod = getLatestRunDate();
        LocalDate insightsLastmod = LocalDate.now();

        List<SitemapEntry> entries = new ArrayList<>(List.of(
                new SitemapEntry(baseUrl + "/", HOME_LASTMOD, "monthly", "1.0"),
                new SitemapEntry(baseUrl + "/guide", GUIDE_LASTMOD, "monthly", "0.9"),
                new SitemapEntry(baseUrl + "/privacy", PRIVACY_LASTMOD, "yearly", "0.4"),
                new SitemapEntry(baseUrl + "/terms", TERMS_LASTMOD, "yearly", "0.4"),
                new SitemapEntry(baseUrl + "/insights/trends", insightsLastmod, "weekly", "0.8"),
                new SitemapEntry(baseUrl + "/use-cases/friend-meetup", USE_CASE_LASTMOD, "monthly", "0.7"),
                new SitemapEntry(baseUrl + "/use-cases/team-meeting", USE_CASE_LASTMOD, "monthly", "0.7"),
                new SitemapEntry(baseUrl + "/use-cases/travel-planning", USE_CASE_LASTMOD, "monthly", "0.7"),
                new SitemapEntry(baseUrl + "/use-cases/study-group", USE_CASE_LASTMOD, "monthly", "0.7"),
                new SitemapEntry(baseUrl + "/faq", FAQ_LASTMOD, "monthly", "0.8"),
                new SitemapEntry(baseUrl + "/tools/date-diff", TOOL_LASTMOD, "monthly", "0.7"),
                new SitemapEntry(baseUrl + "/runners", runnerLastmod, "weekly", "0.8"),
                new SitemapEntry(baseUrl + "/runners/runs", runnerLastmod, "weekly", "0.7"),
                new SitemapEntry(baseUrl + "/runners/members", runnerLastmod, "weekly", "0.7"),
                new SitemapEntry(baseUrl + "/runners/announce", ANNOUNCE_LASTMOD, "monthly", "0.5")
        ));

        // 개별 런 상세 페이지 동적 추가
        if (runRepository != null) {
            runRepository.findAll().forEach(run ->
                entries.add(new SitemapEntry(
                        baseUrl + "/runners/runs/" + run.getId(),
                        run.getCreatedAt().toLocalDate(),
                        "monthly",
                        "0.6"
                ))
            );
        }

        return entries;
    }

    private LocalDate getLatestRunDate() {
        if (runRepository == null) {
            return HOME_LASTMOD;
        }
        return runRepository.findAllOrderByDateDesc().stream()
                .findFirst()
                .map(run -> run.getCreatedAt().toLocalDate())
                .orElse(HOME_LASTMOD);
    }

    public String generateSitemapXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (SitemapEntry entry : getSitemapEntries()) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(entry.loc()).append("</loc>\n");
            xml.append("    <lastmod>").append(entry.lastmod()).append("</lastmod>\n");
            xml.append("    <changefreq>").append(entry.changefreq()).append("</changefreq>\n");
            xml.append("    <priority>").append(entry.priority()).append("</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
