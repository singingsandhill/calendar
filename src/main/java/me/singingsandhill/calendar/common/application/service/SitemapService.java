package me.singingsandhill.calendar.common.application.service;

import me.singingsandhill.calendar.common.application.dto.SitemapEntry;
import me.singingsandhill.calendar.runner.domain.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Dynamic sitemap.xml generation service.
 * Generates sitemap with real-time lastmod dates based on content updates.
 */
@Service
public class SitemapService {

    private final String baseUrl;
    private final RunRepository runRepository;
    private final LocalDate startupDate;

    public SitemapService(
            @Value("${app.base-url:https://datedate.site}") String baseUrl,
            @Autowired(required = false) RunRepository runRepository
    ) {
        this.baseUrl = baseUrl;
        this.runRepository = runRepository;
        this.startupDate = LocalDate.now();
    }

    public List<SitemapEntry> getSitemapEntries() {
        LocalDate runnerLastmod = getLatestRunDate();
        LocalDate insightsLastmod = LocalDate.now();

        return List.of(
                new SitemapEntry(baseUrl + "/", startupDate, "monthly", "1.0"),
                new SitemapEntry(baseUrl + "/start", startupDate, "monthly", "0.9"),
                new SitemapEntry(baseUrl + "/insights", insightsLastmod, "weekly", "0.8"),
                new SitemapEntry(baseUrl + "/insights/trends", insightsLastmod, "weekly", "0.8"),
                new SitemapEntry(baseUrl + "/insights/stats", insightsLastmod, "weekly", "0.7"),
                new SitemapEntry(baseUrl + "/runners", runnerLastmod, "weekly", "0.8"),
                new SitemapEntry(baseUrl + "/runners/runs", runnerLastmod, "weekly", "0.7"),
                new SitemapEntry(baseUrl + "/runners/members", runnerLastmod, "weekly", "0.7")
        );
    }

    private LocalDate getLatestRunDate() {
        if (runRepository == null) {
            return startupDate;
        }
        return runRepository.findAllOrderByDateDesc().stream()
                .findFirst()
                .map(run -> run.getCreatedAt().toLocalDate())
                .orElse(startupDate);
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
