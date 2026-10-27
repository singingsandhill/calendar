package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * MyPageController 는 {@code Clock} 을 생성자로 주입받는다. RecapControllerTest 와
 * 동일한 이유로 (운영 Clock 빈은 @WebMvcTest 슬라이스에 스캔되지 않음) 슬라이스 전용
 * {@code @TestConfiguration} 으로 고정 Clock 을 하나만 공급한다.
 */
@WebMvcTest(MyPageController.class)
@Import({CorsConfig.class, SecurityConfig.class, MyPageControllerTest.FixedClockConfig.class})
class MyPageControllerTest {

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
    private OwnerService ownerService;

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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kakaoUser() {
        return oauth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .attributes(attrs -> {
                    attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L);
                    attrs.put(KakaoOAuth2UserService.ATTR_APP_NICKNAME, "지수");
                    attrs.put(KakaoOAuth2UserService.ATTR_APP_PROFILE_IMAGE, "https://img.example/p.jpg");
                });
    }

    @Test
    @DisplayName("로그인 사용자는 마이페이지를 본다 (헤더 프로필 블록 포함 렌더)")
    void rendersMyPageForKakaoUser() throws Exception {
        when(ownerService.getOwnersOf(42L)).thenReturn(
                List.of(new Owner("my-crew", LocalDateTime.now(), List.of(), 42L)));
        when(seoService.getMyPageSeo()).thenReturn(SeoMetadata.builder().title("마이페이지").build());

        mockMvc.perform(get("/me").with(kakaoUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("me/mypage"))
                .andExpect(content().string(containsString("지수")));
    }

    @Test
    @DisplayName("일정 페이지는 카드로 렌더되고 일정 수는 en 복수형 ChoiceFormat 으로 나온다")
    void rendersOwnerCardsWithScheduleCount() throws Exception {
        when(ownerService.getOwnersOf(42L)).thenReturn(List.of(
                new Owner("my-crew", LocalDateTime.now(), List.of(new Schedule("my-crew", 2026, 10, 7)), 42L),
                new Owner("study-club", LocalDateTime.now(), List.of(), 42L)));
        when(seoService.getMyPageSeo()).thenReturn(SeoMetadata.builder().title("My Page").build());

        mockMvc.perform(get("/me").param("lang", "en").with(kakaoUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"schedule-card\"")))
                .andExpect(content().string(containsString(">1 schedule<")))
                .andExpect(content().string(containsString(">No schedules<")));
    }

    @Test
    @DisplayName("연결된 일정 페이지가 없으면 빈 상태 카드에 일정 만들기 버튼을 보여 준다")
    void rendersEmptyStateWithCreateButton() throws Exception {
        when(ownerService.getOwnersOf(42L)).thenReturn(List.of());
        when(seoService.getMyPageSeo()).thenReturn(SeoMetadata.builder().title("마이페이지").build());
        when(localeLinks.href("/#start-form")).thenReturn("/#start-form");

        mockMvc.perform(get("/me").with(kakaoUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state\"")))
                .andExpect(content().string(containsString("href=\"/#start-form\" class=\"btn btn-primary\"")));
    }

    @Test
    @DisplayName("익명 사용자는 로그인 페이지로 리다이렉트된다")
    void redirectsAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/**/login"));
    }
}
