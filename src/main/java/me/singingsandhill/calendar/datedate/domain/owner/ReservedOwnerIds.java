package me.singingsandhill.calendar.datedate.domain.owner;

import java.util.Set;

/**
 * Owner ID로 사용 금지된 토큰. 정적 라우트(`/insights`, `/guide` 등)와 사용자 ID
 * 와일드카드(`/{ownerId}`)가 같은 네임스페이스를 공유하므로, 이 목록의 토큰을
 * Owner ID로 점유하면 마케팅·기능 페이지가 사용자 대시보드로 가려진다.
 */
public final class ReservedOwnerIds {

    public static final Set<String> RESERVED = Set.of(
            // 사용자 ID로 점유되면 안 되는 일반 토큰
            "home", "create-schedule", "guide", "insights", "trends",
            "privacy", "terms", "feedback", "api", "admin", "login",
            "logout", "signup", "settings", "dashboard", "random",
            "trading", "stock", "runners",
            // 이 프로젝트의 추가 정적 라우트 / 도메인
            "faq", "start", "tools", "use-cases",
            // SecurityConfig permitAll 또는 외부 도구가 점유하는 경로
            "about", "privacy-policy", "h2-console", "index",
            // Spring 기본 에러 전달 경로
            "error",
            // 정적 리소스 디렉터리 (확장자 없이 들어오면 와일드카드로 빠짐)
            "css", "js", "image", "images",
            // 확장자 없는 정적 파일 별칭 (사용자가 추측해서 들어오는 경로)
            "sitemap", "robots", "manifest", "favicon", "ads"
    );

    private ReservedOwnerIds() {
    }

    public static boolean isReserved(String ownerId) {
        return ownerId != null && RESERVED.contains(ownerId.toLowerCase());
    }
}
