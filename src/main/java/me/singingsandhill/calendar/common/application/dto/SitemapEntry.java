package me.singingsandhill.calendar.common.application.dto;

import java.time.LocalDate;

/**
 * Sitemap URL entry for dynamic sitemap generation.
 */
public record SitemapEntry(
        String loc,
        LocalDate lastmod,
        String changefreq,
        String priority
) {
}
