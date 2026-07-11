package me.singingsandhill.calendar.datedate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class KakaoProfileTest {

    @Test
    @DisplayName("kakao_account.profile 에서 닉네임·프로필 이미지를 파싱한다")
    void parsesKakaoAccountProfile() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "kakao_account", Map.of("profile", Map.of(
                        "nickname", "지수",
                        "profile_image_url", "https://img.kakaocdn.net/p.jpg")),
                "properties", Map.of("nickname", "legacy"));

        KakaoProfile profile = KakaoProfile.from(attributes);

        assertThat(profile.kakaoId()).isEqualTo(12345L);
        assertThat(profile.nickname()).isEqualTo("지수");
        assertThat(profile.profileImageUrl()).isEqualTo("https://img.kakaocdn.net/p.jpg");
    }

    @Test
    @DisplayName("kakao_account.profile 이 없으면 properties 로 폴백한다")
    void fallsBackToProperties() {
        Map<String, Object> attributes = Map.of(
                "id", 99L,
                "properties", Map.of("nickname", "프로퍼티닉", "profile_image", "https://img/p2.jpg"));

        KakaoProfile profile = KakaoProfile.from(attributes);

        assertThat(profile.nickname()).isEqualTo("프로퍼티닉");
        assertThat(profile.profileImageUrl()).isEqualTo("https://img/p2.jpg");
    }

    @Test
    @DisplayName("id (Integer 타입 포함) 를 Long 으로 정규화한다")
    void normalizesIntegerId() {
        KakaoProfile profile = KakaoProfile.from(Map.of("id", 777));

        assertThat(profile.kakaoId()).isEqualTo(777L);
        assertThat(profile.nickname()).isNull();
        assertThat(profile.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("id 가 없으면 OAuth2AuthenticationException")
    void throwsWhenIdMissing() {
        assertThatThrownBy(() -> KakaoProfile.from(Map.of("properties", Map.of())))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
