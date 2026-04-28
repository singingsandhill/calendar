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
 * sitemap.xml 동적 생성.
 *
 * 공개 SEO 페이지는 한/영 양방향 (bilingual=true) 으로 기록되어 각 언어별로 별도 {@code <url>} 블록이
 * 생성되고 xhtml:link rel=alternate 엔트리를 포함한다. Runner 및 UGC 계열은 한국어 전용으로 단일 URL.
 */
@Service
public class SitemapService {

    // 콘텐츠 최종 수정일 — 실제 페이지 내용이 바뀔 때마다 갱신
    private static final LocalDate HOME_LASTMOD     = LocalDate.of(2026, 4, 24);
    private static final LocalDate GUIDE_LASTMOD    = LocalDate.of(2026, 4, 24);
    private static final LocalDate PRIVACY_LASTMOD  = LocalDate.of(2026, 4, 6);
    private static final LocalDate TERMS_LASTMOD    = LocalDate.of(2026, 4, 6);
    private static final LocalDate USE_CASE_LASTMOD = LocalDate.of(2026, 4, 24);
    private static final LocalDate ANNOUNCE_LASTMOD = LocalDate.of(2026, 4, 6);
    private static final LocalDate FAQ_LASTMOD      = LocalDate.of(2026, 4, 24);
    private static final LocalDate TOOL_LASTMOD     = LocalDate.of(2026, 4, 24);

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
                new SitemapEntry(baseUrl + "/", HOME_LASTMOD, "monthly", "1.0", true),
                new SitemapEntry(baseUrl + "/guide", GUIDE_LASTMOD, "monthly", "0.9", true),
                new SitemapEntry(baseUrl + "/privacy", PRIVACY_LASTMOD, "yearly", "0.4", true),
                new SitemapEntry(baseUrl + "/terms", TERMS_LASTMOD, "yearly", "0.4", true),
                new SitemapEntry(baseUrl + "/insights/trends", insightsLastmod, "weekly", "0.8", true),
                new SitemapEntry(baseUrl + "/use-cases/friend-meetup", USE_CASE_LASTMOD, "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/team-meeting", USE_CASE_LASTMOD, "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/travel-planning", USE_CASE_LASTMOD, "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/study-group", USE_CASE_LASTMOD, "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/faq", FAQ_LASTMOD, "monthly", "0.8", true),
                new SitemapEntry(baseUrl + "/tools/date-diff", TOOL_LASTMOD, "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/runners", runnerLastmod, "weekly", "0.8", false),
                new SitemapEntry(baseUrl + "/runners/runs", runnerLastmod, "weekly", "0.7", false),
                new SitemapEntry(baseUrl + "/runners/members", runnerLastmod, "weekly", "0.7", false),
                new SitemapEntry(baseUrl + "/runners/announce", ANNOUNCE_LASTMOD, "monthly", "0.5", false)
        ));

        // 개별 런 상세 페이지 동적 추가
        if (runRepository != null) {
            runRepository.findAll().forEach(run ->
                entries.add(new SitemapEntry(
                        baseUrl + "/runners/runs/" + run.getId(),
                        run.getCreatedAt().toLocalDate(),
                        "monthly",
                        "0.6",
                        false
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
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"")
           .append(" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");

        for (SitemapEntry entry : getSitemapEntries()) {
            if (entry.bilingual()) {
                String koUrl = entry.loc();
                String enUrl = appendLangEn(entry.loc());
                appendUrl(xml, koUrl, entry, koUrl, enUrl);
                appendUrl(xml, enUrl, entry, koUrl, enUrl);
            } else {
                appendUrl(xml, entry.loc(), entry, null, null);
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /** 단일 {@code <url>} 블록을 추가한다. ko/en URL 이 제공되면 xhtml:link 대체 URL 도 기록. */
    private void appendUrl(StringBuilder xml, String loc, SitemapEntry entry,
                           String koUrl, String enUrl) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        xml.append("    <lastmod>").append(entry.lastmod()).append("</lastmod>\n");
        xml.append("    <changefreq>").append(entry.changefreq()).append("</changefreq>\n");
        xml.append("    <priority>").append(entry.priority()).append("</priority>\n");
        if (koUrl != null && enUrl != null) {
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"ko\" href=\"").append(koUrl).append("\"/>\n");
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"").append(enUrl).append("\"/>\n");
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"").append(koUrl).append("\"/>\n");
        }
        xml.append("  </url>\n");
    }

    private static String appendLangEn(String url) {
        return url + (url.contains("?") ? "&" : "?") + "lang=en";
    }
}
