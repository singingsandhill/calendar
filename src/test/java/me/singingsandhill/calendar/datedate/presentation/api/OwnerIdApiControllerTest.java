package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(OwnerIdApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class OwnerIdApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

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
    @DisplayName("GET /api/owner-ids/random 은 로그인 없이 미사용 ID 를 준다")
    void randomReturnsAvailableIdWithoutAuthentication() throws Exception {
        when(ownerService.generateAvailableOwnerId()).thenReturn("brave-otter-4821");

        mockMvc.perform(get("/api/owner-ids/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("brave-otter-4821"));
    }

    @Test
    @DisplayName("/api/owners/** 인터셉터를 타지 않는다 — 'random' 은 예약어라 그 아래였다면 400 이 된다")
    void livesOutsideTheOwnerPathInterceptorNamespace() throws Exception {
        when(ownerService.generateAvailableOwnerId()).thenReturn("sunny-pine-1057");

        mockMvc.perform(get("/api/owner-ids/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("sunny-pine-1057"));
    }
}
