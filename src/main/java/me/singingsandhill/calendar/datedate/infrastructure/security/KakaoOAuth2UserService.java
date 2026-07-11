package me.singingsandhill.calendar.datedate.infrastructure.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import me.singingsandhill.calendar.datedate.application.service.AppUserService;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;

/**
 * 카카오 사용자 정보를 AppUser 로 upsert 하고 ROLE_USER 프린시펄을 만든다.
 * 내부 userId 를 attributes 에 실어 컨트롤러가 추가 조회 없이 쓰게 한다.
 */
@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    public static final String ATTR_APP_USER_ID = "appUserId";
    public static final String ATTR_APP_NICKNAME = "appNickname";
    public static final String ATTR_APP_PROFILE_IMAGE = "appProfileImage";

    private final AppUserService appUserService;

    public KakaoOAuth2UserService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        KakaoProfile profile = KakaoProfile.from(oauth2User.getAttributes());

        AppUser user = appUserService.upsertKakaoUser(
                profile.kakaoId(), profile.nickname(), profile.profileImageUrl());

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put(ATTR_APP_USER_ID, user.getId());
        attributes.put(ATTR_APP_NICKNAME, user.getNickname());
        attributes.put(ATTR_APP_PROFILE_IMAGE, user.getProfileImageUrl());

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
    }
}
