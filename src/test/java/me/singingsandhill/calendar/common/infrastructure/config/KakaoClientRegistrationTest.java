package me.singingsandhill.calendar.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADR common/security/0004: 카카오는 client_secret 을 POST body 로만 받는다.
 * client_secret_post 가 아니면 토큰 교환이 KOE010 으로 실패하므로 회귀 가드.
 */
@SpringBootTest
@ActiveProfiles("test")
class KakaoClientRegistrationTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("kakao 클라이언트 등록은 공식 엔드포인트와 client_secret_post 를 사용한다")
    void kakaoRegistrationMatchesOfficialDocs() {
        ClientRegistration kakao = clientRegistrationRepository.findByRegistrationId("kakao");

        assertThat(kakao).isNotNull();
        assertThat(kakao.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://kauth.kakao.com/oauth/authorize");
        assertThat(kakao.getProviderDetails().getTokenUri())
                .isEqualTo("https://kauth.kakao.com/oauth/token");
        assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUri())
                .isEqualTo("https://kapi.kakao.com/v2/user/me");
        assertThat(kakao.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("id");
        assertThat(kakao.getClientAuthenticationMethod())
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        assertThat(kakao.getScopes()).containsExactlyInAnyOrder("profile_nickname", "profile_image");
        assertThat(kakao.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/kakao");
    }
}
