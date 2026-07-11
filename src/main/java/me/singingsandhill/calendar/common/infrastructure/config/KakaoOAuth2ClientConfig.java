package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * 카카오 ClientRegistration 수동 등록 (ADR common/security/0004).
 * spring.security.oauth2.client.* 프로퍼티 대신 빈으로 등록하는 이유:
 * 프로퍼티 방식은 모든 @WebMvcTest 슬라이스에 OAuth2 자동설정을 끌어들여
 * HttpSecurity 부재로 컨텍스트 로드를 깨뜨린다 (@Configuration 은 슬라이스에 스캔되지 않음).
 * 카카오 공식 문서: 토큰 엔드포인트는 client_secret 을 POST body 로만 받는다 → client_secret_post.
 */
@Configuration
public class KakaoOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${kakao.oauth2.client-id}") String clientId,
            @Value("${kakao.oauth2.client-secret}") String clientSecret) {
        ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/kakao")
                .scope("profile_nickname", "profile_image")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        return new InMemoryClientRegistrationRepository(kakao);
    }
}
