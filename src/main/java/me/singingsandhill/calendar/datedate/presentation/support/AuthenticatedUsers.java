package me.singingsandhill.calendar.datedate.presentation.support;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;

/** 컨트롤러에서 현재 카카오 로그인 사용자의 내부 userId 를 꺼내는 헬퍼. 비로그인·어드민 세션이면 empty. */
public final class AuthenticatedUsers {

    private AuthenticatedUsers() {
    }

    public static Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return Optional.empty();
        }
        Object id = oauth2User.getAttribute(KakaoOAuth2UserService.ATTR_APP_USER_ID);
        return (id instanceof Number number) ? Optional.of(number.longValue()) : Optional.empty();
    }
}
