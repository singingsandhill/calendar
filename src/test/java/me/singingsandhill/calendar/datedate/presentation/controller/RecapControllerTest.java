package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.RecapService;
import me.singingsandhill.calendar.datedate.application.service.RecapShareService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * RecapController 는 {@code Clock} 을 생성자로 주입받는다. 운영 컨텍스트에서는
 * {@code StockSchedulerConfig#stockClock()} 이 앱 전역 단일 Clock 빈을 제공하지만,
 * {@code @WebMvcTest} 슬라이스는 명시적으로 {@code @Import} 하지 않은 {@code @Configuration}
 * 을 스캔하지 않아 해당 빈이 없다 — 새 Clock 빈을 추가하면 운영 컨텍스트에서 중복(둘 다
 * {@code @ConditionalOnMissingBean} 없이 존재 시 충돌) 되므로, 이 슬라이스 전용
 * {@code @TestConfiguration} 으로 고정 Clock 을 하나만 공급한다.
 */
@WebMvcTest(RecapController.class)
@Import({CorsConfig.class, SecurityConfig.class, RecapControllerTest.FixedClockConfig.class})
class RecapControllerTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecapService recapService;

    @MockitoBean
    private RecapShareService recapShareService;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean(name = "localeLinks")
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

    private static RecapDto sampleRecap() {
        return new RecapDto(2026, "지수", 3, 12, 2, 9, "FRIDAY", 5,
                List.of("성수 카페"), List.of("마라탕"), List.of("민준"), false);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kakaoUser() {
        return oauth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L));
    }

    @Test
    @DisplayName("로그인 사용자는 연도 recap 페이지를 본다")
    void rendersRecapForKakaoUser() throws Exception {
        when(recapService.buildRecap(42L, 2026)).thenReturn(sampleRecap());
        when(seoService.getRecapSeo(2026)).thenReturn(SeoMetadata.builder().title("리캡").build());

        mockMvc.perform(get("/recap/2026").with(kakaoUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("recap/recap"));
    }

    @Test
    @DisplayName("/recap 은 올해 recap 으로 리다이렉트한다")
    void redirectsToCurrentYear() throws Exception {
        mockMvc.perform(get("/recap").with(kakaoUser()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("공유 토큰 생성은 멱등이고 recap 페이지로 돌아간다")
    void createsShareAndRedirectsBack() throws Exception {
        when(recapShareService.getOrCreateShare(42L, 2026)).thenReturn(
                new RecapShare(1L, 42L, 2026, "abc-token", LocalDateTime.now()));

        mockMvc.perform(post("/recap/2026/share").with(kakaoUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recap/2026"));
    }

    @Test
    @DisplayName("공유 페이지는 무인증으로 렌더링된다")
    void sharePageIsPublic() throws Exception {
        when(recapShareService.getByToken("abc-token")).thenReturn(
                new RecapShare(1L, 42L, 2026, "abc-token", LocalDateTime.now()));
        when(recapService.buildRecap(42L, 2026)).thenReturn(sampleRecap());
        when(seoService.getRecapShareSeo(eq("지수"), anyInt()))
                .thenReturn(SeoMetadata.builder().title("리캡 공유").robots("noindex, nofollow").build());

        mockMvc.perform(get("/recap/share/abc-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("recap/share"));
    }

    @Test
    @DisplayName("없는 공유 토큰은 404")
    void missingShareTokenIs404() throws Exception {
        when(recapShareService.getByToken("nope"))
                .thenThrow(new RecapShareNotFoundException("nope"));

        mockMvc.perform(get("/recap/share/nope"))
                .andExpect(status().isNotFound());
    }
}
