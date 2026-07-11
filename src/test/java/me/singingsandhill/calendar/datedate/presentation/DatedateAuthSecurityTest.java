package me.singingsandhill.calendar.datedate.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.datedate.presentation.controller.HomeController;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * ADR common/security/0004: 카카오 사용자 영역의 접근 규칙·진입점 분리 회귀 가드.
 * 컨트롤러 도달 전 시큐리티 레이어 동작만 검증한다 (핸들러 미존재 경로는 인가 통과 시 404).
 */
@WebMvcTest(HomeController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class DatedateAuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private PopularityService popularityService;

    @MockitoBean
    private InsightsService insightsService;

    @MockitoBean
    private LocaleLinks localeLinks;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @Test
    @DisplayName("미인증 /me 접근은 카카오 로그인 페이지(/login)로 리다이렉트된다")
    void unauthenticatedMeRedirectsToUserLogin() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/**/login"));
    }

    @Test
    @DisplayName("미인증 /recap/2026 접근은 /login 으로 리다이렉트된다")
    void unauthenticatedRecapRedirectsToUserLogin() throws Exception {
        mockMvc.perform(get("/recap/2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/**/login"));
    }

    @Test
    @DisplayName("미인증 러너 어드민 접근은 여전히 어드민 로그인으로 리다이렉트된다")
    void unauthenticatedAdminRedirectsToAdminLogin() throws Exception {
        mockMvc.perform(get("/runners/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/**/runners/admin/login"));
    }

    @Test
    @DisplayName("recap 공유 링크는 무인증으로 인가를 통과한다 (핸들러 미존재 → 404, 302 아님)")
    void shareLinkIsPubliclyAuthorized() throws Exception {
        mockMvc.perform(get("/recap/share/some-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("카카오 사용자(ROLE_USER)는 트레이딩 대시보드에 접근할 수 없다 (403)")
    void kakaoUserCannotAccessTrading() throws Exception {
        mockMvc.perform(get("/trading")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카카오 사용자(ROLE_USER)는 /me 인가를 통과한다 (핸들러 미존재 → 404, 403 아님)")
    void kakaoUserPassesMeAuthorization() throws Exception {
        mockMvc.perform(get("/me")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }
}
