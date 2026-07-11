package me.singingsandhill.calendar.datedate.infrastructure.security;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * 카카오 /v2/user/me 응답 파싱 결과.
 * 공식 문서: id(회원번호, Long) 필수. 닉네임·프로필 이미지는
 * kakao_account.profile.{nickname, profile_image_url} 우선, properties.{nickname, profile_image} 폴백.
 */
public record KakaoProfile(Long kakaoId, String nickname, String profileImageUrl) {

    @SuppressWarnings("unchecked")
    public static KakaoProfile from(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        if (!(id instanceof Number number)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response"), "kakao user id is missing");
        }
        Map<String, Object> account = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        Map<String, Object> properties = (Map<String, Object>) attributes.getOrDefault("properties", Map.of());

        String nickname = firstNonBlank(
                (String) profile.get("nickname"),
                (String) properties.get("nickname"));
        String imageUrl = firstNonBlank(
                (String) profile.get("profile_image_url"),
                (String) properties.get("profile_image"));

        return new KakaoProfile(number.longValue(), nickname, imageUrl);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
