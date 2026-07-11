package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.UserActivityService;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

/**
 * ADR datedate/domain/0005: 활동 이벤트는 로그인 세션에서만 기록되고,
 * 익명 요청 경로는 완전히 무변경이어야 한다.
 */
@WebMvcTest(ParticipantApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class ParticipantApiActivityRecordingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipantService participantService;

    @MockitoBean
    private UserActivityService userActivityService;

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

    private Participant participant() {
        return new Participant(5L, 3L, "지수", ParticipantColor.ofIndex(0), List.of(1, 2), LocalDateTime.now());
    }

    @Test
    @DisplayName("카카오 로그인 사용자의 참여자 추가는 PARTICIPATION 이벤트를 기록한다")
    void recordsParticipationForKakaoUser() throws Exception {
        when(participantService.addParticipant(eq(3L), eq("지수"))).thenReturn(participant());

        mockMvc.perform(post("/api/schedules/3/participants")
                        .with(oauth2Login()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .attributes(attrs -> attrs.put(KakaoOAuth2UserService.ATTR_APP_USER_ID, 42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"지수\"}"))
                .andExpect(status().isCreated());

        verify(userActivityService).record(42L, ActivityType.PARTICIPATION, 3L, 5L, "지수");
    }

    @Test
    @DisplayName("익명 참여자 추가는 이벤트를 기록하지 않는다 (기존 플로우 무변경)")
    void doesNotRecordForAnonymous() throws Exception {
        when(participantService.addParticipant(eq(3L), eq("지수"))).thenReturn(participant());

        mockMvc.perform(post("/api/schedules/3/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"지수\"}"))
                .andExpect(status().isCreated());

        verify(userActivityService, never()).record(anyLong(), any(), anyLong(), anyLong(), anyString());
    }
}
