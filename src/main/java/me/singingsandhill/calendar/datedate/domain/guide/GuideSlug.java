package me.singingsandhill.calendar.datedate.domain.guide;

import java.time.LocalDate;

/**
 * /guides 기사 1건의 메타데이터.
 *
 * <p>{@code published}/{@code modified} 는 사이트맵 lastmod, Article JSON-LD 의
 * datePublished/dateModified, 화면 바이라인 3곳의 단일 진실원이다 — 기사 본문을
 * 수정하면 반드시 {@link GuideSlugs} 의 {@code modified} 를 함께 갱신한다
 * (ADR common/seo/0003 신뢰 가능한 lastmod 정책).
 */
public record GuideSlug(String slug, LocalDate published, LocalDate modified) {
}
