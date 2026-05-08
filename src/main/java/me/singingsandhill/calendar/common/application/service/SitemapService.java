package me.singingsandhill.calendar.common.application.service;

import me.singingsandhill.calendar.common.application.dto.SitemapEntry;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;
import me.singingsandhill.calendar.runner.domain.Run;
import me.singingsandhill.calendar.runner.domain.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * sitemap.xml 동적 생성.
 *
 * <p>공개 SEO 페이지는 한/영 양방향 (bilingual=true) 으로 기록되어 각 언어별로 별도 {@code <url>} 블록이
 * 생성되고 xhtml:link rel=alternate 엔트리를 포함한다. Runner 및 UGC 계열은 한국어 전용으로 단일 URL.
 *
 * <p>lastmod 정책:
 * <ul>
 *   <li>정적 페이지: {@link BuildProperties#getTime()} (배포 시각). 빌드 시 {@code build-info.properties} 미생성이면 startup 시각 폴백.</li>
 *   <li>insights/trends: 인기 데이터(Location/Menu) 의 가장 최근 createdAt. 데이터 없으면 빌드 시각.</li>
 *   <li>Run 상세: 해당 Run 의 createdAt (도메인에 updatedAt 없음).</li>
 * </ul>
 */
@Service
public class SitemapService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final String baseUrl;
    private final OffsetDateTime buildTime;
    private final RunRepository runRepository;
    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;

    public SitemapService(
            @Value("${app.base-url:https://datedate.site}") String baseUrl,
            @Autowired(required = false) BuildProperties buildProperties,
            @Autowired(required = false) RunRepository runRepository,
            @Autowired(required = false) LocationRepository locationRepository,
            @Autowired(required = false) MenuRepository menuRepository
    ) {
        this.baseUrl = baseUrl;
        this.buildTime = resolveBuildTime(buildProperties);
        this.runRepository = runRepository;
        this.locationRepository = locationRepository;
        this.menuRepository = menuRepository;
    }

    private static OffsetDateTime resolveBuildTime(BuildProperties buildProperties) {
        Instant instant = buildProperties != null ? buildProperties.getTime() : Instant.now();
        return instant.atZone(KST).toOffsetDateTime();
    }

    public List<SitemapEntry> getSitemapEntries() {
        OffsetDateTime insightsLastmod = computeInsightsLastmod();

        List<SitemapEntry> entries = new ArrayList<>(List.of(
                new SitemapEntry(baseUrl + "/",                            buildTime,        "monthly", "1.0", true),
                new SitemapEntry(baseUrl + "/guide",                       buildTime,        "monthly", "0.9", true),
                new SitemapEntry(baseUrl + "/privacy",                     buildTime,        "yearly",  "0.4", true),
                new SitemapEntry(baseUrl + "/terms",                       buildTime,        "yearly",  "0.4", true),
                new SitemapEntry(baseUrl + "/insights/trends",             insightsLastmod, "weekly",  "0.8", true),
                new SitemapEntry(baseUrl + "/use-cases/friend-meetup",     buildTime,        "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/team-meeting",      buildTime,        "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/travel-planning",   buildTime,        "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/use-cases/study-group",       buildTime,        "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/faq",                         buildTime,        "monthly", "0.8", true),
                new SitemapEntry(baseUrl + "/tools/date-diff",             buildTime,        "monthly", "0.7", true),
                new SitemapEntry(baseUrl + "/runners",                     latestRunTime(), "weekly",  "0.8", false),
                new SitemapEntry(baseUrl + "/runners/runs",                latestRunTime(), "weekly",  "0.7", false),
                new SitemapEntry(baseUrl + "/runners/members",             latestRunTime(), "weekly",  "0.7", false),
                new SitemapEntry(baseUrl + "/runners/announce",            buildTime,        "monthly", "0.5", false)
        ));

        if (runRepository != null) {
            runRepository.findAll().forEach(run ->
                entries.add(new SitemapEntry(
                        baseUrl + "/runners/runs/" + run.getId(),
                        toOffsetDateTime(run.getCreatedAt()),
                        "monthly",
                        "0.6",
                        false
                ))
            );
        }

        return entries;
    }

    /** 인기 콘텐츠가 마지막으로 추가된 시각. 데이터가 없으면 배포 시각으로 대체 (LocalDate.now() 같은 거짓 신호 금지). */
    private OffsetDateTime computeInsightsLastmod() {
        Optional<LocalDateTime> latestLocation = locationRepository != null
                ? locationRepository.findLatestActivity()
                : Optional.empty();
        Optional<LocalDateTime> latestMenu = menuRepository != null
                ? menuRepository.findLatestActivity()
                : Optional.empty();

        return latestLocation
                .map(l -> latestMenu.map(m -> m.isAfter(l) ? m : l).orElse(l))
                .or(() -> latestMenu)
                .map(this::toOffsetDateTime)
                .orElse(buildTime);
    }

    private OffsetDateTime latestRunTime() {
        if (runRepository == null) return buildTime;
        return runRepository.findAllOrderByDateDesc().stream()
                .findFirst()
                .map(Run::getCreatedAt)
                .map(this::toOffsetDateTime)
                .orElse(buildTime);
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        return ldt.atZone(KST).toOffsetDateTime();
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
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(ISO.format(entry.lastmod())).append("</lastmod>\n");
        xml.append("    <changefreq>").append(entry.changefreq()).append("</changefreq>\n");
        xml.append("    <priority>").append(entry.priority()).append("</priority>\n");
        if (koUrl != null && enUrl != null) {
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"ko\" href=\"").append(escapeXml(koUrl)).append("\"/>\n");
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"").append(escapeXml(enUrl)).append("\"/>\n");
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"").append(escapeXml(koUrl)).append("\"/>\n");
        }
        xml.append("  </url>\n");
    }

    private static String appendLangEn(String url) {
        return url + (url.contains("?") ? "&" : "?") + "lang=en";
    }

    /** XML attribute/content 에 들어가는 5개 미리 정의된 엔티티만 escape 한다. */
    static String escapeXml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&'  -> sb.append("&amp;");
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
