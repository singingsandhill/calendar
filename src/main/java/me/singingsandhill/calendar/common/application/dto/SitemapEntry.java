package me.singingsandhill.calendar.common.application.dto;

import java.time.OffsetDateTime;

/**
 * Sitemap URL entry for dynamic sitemap generation.
 *
 * @param lastmod ISO 8601 풀 정밀도. {@link java.time.LocalDate} 만 사용하면 같은 날 두 번 갱신 시 신호 손실.
 * @param bilingual 이 URL 이 한/영 양쪽으로 인덱싱 가능할 때 true.
 *                  true 이면 SitemapService 가 ko/en 각각에 대한 {@code <url>} 블록을 생성하고
 *                  xhtml:link hreflang 대체 링크를 포함시킨다.
 */
public record SitemapEntry(
        String loc,
        OffsetDateTime lastmod,
        String changefreq,
        String priority,
        boolean bilingual
) {
    public SitemapEntry(String loc, OffsetDateTime lastmod, String changefreq, String priority) {
        this(loc, lastmod, changefreq, priority, false);
    }
}
