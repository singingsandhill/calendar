package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * 공통 api.js 의 오류 문구는 fragments/scripts 가 API_ERROR_MESSAGES(메시지 키 api.error.*)로 로케일별 주입한다 —
 * JS 에 한국어를 하드코딩하면 en 화면에도 한국어 토스트가 뜬다. scripts fragment 를 쓰는 공개 페이지(/login)로 확인.
 */
@WebMvcTest(AuthController.class)
@Import({CorsConfig.class, SecurityConfig.class, LocaleLinks.class})
class ApiErrorMessagesInjectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeoService seoService;

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
    @DisplayName("en 페이지는 api.js 오류 문구(코드별·대체 문구)를 영어로 주입한다")
    void englishApiErrorMessages() throws Exception {
        when(seoService.getLoginSeo()).thenReturn(SeoMetadata.builder().title("Login").build());

        mockMvc.perform(get("/login").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DUPLICATE_PARTICIPANT: \"A participant with this name already exists\"")))
                .andExpect(content().string(containsString("SCHEDULE_NOT_FOUND: \"Schedule not found\"")))
                .andExpect(content().string(containsString("UNKNOWN: \"Something went wrong\"")));
    }
}
