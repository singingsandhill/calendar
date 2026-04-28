package me.singingsandhill.calendar.common.application.dto;

import java.time.LocalDate;

/**
 * Sitemap URL entry for dynamic sitemap generation.
 *
 * @param bilingual 이 URL 이 한/영 양쪽으로 인덱싱 가능할 때 true.
 *                  true 이면 SitemapService 가 ko/en 각각에 대한 {@code <url>} 블록을 생성하고
 *                  xhtml:link hreflang 대체 링크를 포함시킨다.
 */
public record SitemapEntry(
        String loc,
        LocalDate lastmod,
        String changefreq,
        String priority,
        boolean bilingual
) {
    public SitemapEntry(String loc, LocalDate lastmod, String changefreq, String priority) {
        this(loc, lastmod, changefreq, priority, false);
    }
}
