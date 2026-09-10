package me.singingsandhill.calendar.datedate.domain.guide;

import java.util.List;
import java.util.Optional;

/**
 * /guides 에디토리얼 허브 기사 슬러그의 단일 진실원 (SSOT).
 *
 * <p>여기 등록하면 라우팅({@code GuidesController})·사이트맵·푸터 내비가 자동 반영된다
 * (use-case 의 {@code UseCaseSlugs} 패턴). 슬러그는 반드시 대응 템플릿
 * ({@code templates/guides/<slug>.html})·메시지 키 블록({@code guides.article.<slug>.*})과
 * 같은 커밋에서만 추가한다 — 템플릿 없는 슬러그는 렌더링 시 500 이 된다.
 */
public final class GuideSlugs {

    public static final List<GuideSlug> ALL = List.of(
            new GuideSlug("how-to-pick-a-date",
                    java.time.LocalDate.of(2026, 8, 23), java.time.LocalDate.of(2026, 8, 23)),
            new GuideSlug("scheduling-methods-compared",
                    java.time.LocalDate.of(2026, 8, 23), java.time.LocalDate.of(2026, 8, 23)),
            new GuideSlug("scheduling-etiquette",
                    java.time.LocalDate.of(2026, 8, 23), java.time.LocalDate.of(2026, 8, 23)),
            new GuideSlug("group-poll-best-practices",
                    java.time.LocalDate.of(2026, 8, 23), java.time.LocalDate.of(2026, 8, 23))
    );

    private GuideSlugs() {
    }

    public static List<String> slugs() {
        return ALL.stream().map(GuideSlug::slug).toList();
    }

    public static Optional<GuideSlug> find(String slug) {
        return ALL.stream().filter(g -> g.slug().equals(slug)).findFirst();
    }

    public static boolean contains(String slug) {
        return find(slug).isPresent();
    }
}
