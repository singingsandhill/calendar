package me.singingsandhill.calendar.datedate.domain.usecase;

import java.util.List;

/**
 * Use-case 콘텐츠 페이지 슬러그 단일 진실원.
 *
 * <p>{@code UseCaseController} 가 라우팅 검증에, {@code SitemapService} 가 sitemap URL
 * 생성에, footer/index 그리드가 카드 노출에 동일한 리스트를 참조하도록 한다.
 * 새 슬러그를 추가할 때 이 리스트만 갱신하면 라우팅·사이트맵·footer 가 동시에 반영된다.
 */
public final class UseCaseSlugs {

    public static final List<String> ALL = List.of(
            "friend-meetup",
            "team-meeting",
            "travel-planning",
            "study-group",
            "club-activity"
    );

    private UseCaseSlugs() {
    }
}
