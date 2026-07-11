package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

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
import me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(MeApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class MeApiControllerTest {

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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kakaoUser(long userId) {
        return oauth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, userId));
    }

    @Test
    @DisplayName("로그인 사용자는 미연결 오너를 자기 계정에 연결할 수 있다")
    void linkOwnerSucceeds() throws Exception {
        Owner linked = new Owner("my-crew", LocalDateTime.now(), List.of(), 42L);
        when(ownerService.linkOwnerToUser(eq("my-crew"), eq(42L))).thenReturn(linked);

        mockMvc.perform(post("/api/me/owners/my-crew").with(kakaoUser(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("my-crew"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("타 유저 선점 오너 연결은 409 + OWNER_ALREADY_LINKED")
    void linkOwnerConflicts() throws Exception {
        when(ownerService.linkOwnerToUser(eq("my-crew"), eq(42L)))
                .thenThrow(new OwnerAlreadyLinkedException("my-crew"));

        mockMvc.perform(post("/api/me/owners/my-crew").with(kakaoUser(42L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("미인증 연결 요청은 로그인으로 리다이렉트된다")
    void anonymousLinkIsRedirected() throws Exception {
        mockMvc.perform(post("/api/me/owners/my-crew"))
                .andExpect(status().is3xxRedirection());
    }
}
