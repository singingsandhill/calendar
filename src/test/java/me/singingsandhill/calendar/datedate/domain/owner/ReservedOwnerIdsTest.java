package me.singingsandhill.calendar.datedate.domain.owner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReservedOwnerIdsTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" is reserved")
    @ValueSource(strings = {
            "home", "create-schedule", "guide", "insights", "trends",
            "privacy", "terms", "feedback", "api", "admin", "login",
            "logout", "signup", "settings", "dashboard", "random",
            "trading", "stock", "runners",
            "faq", "start", "tools", "use-cases",
            "about", "privacy-policy", "h2-console", "index",
            "error",
            "css", "js", "image", "images",
            "sitemap", "robots", "manifest", "favicon", "ads"
    })
    @DisplayName("All declared reserved tokens are recognized as reserved")
    void allDeclaredTokensAreReserved(String token) {
        assertThat(ReservedOwnerIds.isReserved(token)).isTrue();
    }

    @Test
    @DisplayName("Reserved set contains exactly the declared tokens (no drift)")
    void reservedSetMatchesExpected() {
        assertThat(ReservedOwnerIds.RESERVED).containsExactlyInAnyOrder(
                "home", "create-schedule", "guide", "insights", "trends",
                "privacy", "terms", "feedback", "api", "admin", "login",
                "logout", "signup", "settings", "dashboard", "random",
                "trading", "stock", "runners",
                "me", "recap", "oauth2",
                "faq", "start", "tools", "use-cases",
                "about", "privacy-policy", "h2-console", "index",
                "error",
                "css", "js", "image", "images",
                "sitemap", "robots", "manifest", "favicon", "ads"
        );
    }

    @Test
    @DisplayName("Mixed-case input is normalized before lookup")
    void mixedCaseIsNormalized() {
        assertThat(ReservedOwnerIds.isReserved("Insights")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("ADMIN")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("Create-Schedule")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "bob-123", "team-x", "user01", "j-smith", "gildong"})
    @DisplayName("Non-reserved IDs are not reserved")
    void nonReservedIdsReturnFalse(String token) {
        assertThat(ReservedOwnerIds.isReserved(token)).isFalse();
    }

    @Test
    @DisplayName("Null and blank inputs are treated as non-reserved")
    void nullAndBlankAreSafe() {
        assertThat(ReservedOwnerIds.isReserved(null)).isFalse();
        assertThat(ReservedOwnerIds.isReserved("")).isFalse();
    }

    @Test
    @DisplayName("카카오 로그인 라우트 토큰(me, recap, oauth2)은 예약어다")
    void kakaoAuthRouteTokensAreReserved() {
        assertThat(ReservedOwnerIds.isReserved("me")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("recap")).isTrue();
        assertThat(ReservedOwnerIds.isReserved("oauth2")).isTrue();
    }
}
