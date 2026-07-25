package me.singingsandhill.calendar.stock.presentation.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;

/**
 * 2026-07-24 리뷰 §3-② / P0-1: 주식 봇 제어(뮤테이션) API 가 무인증으로 노출되면 안 된다.
 *
 * {@code POST /api/stock/bot/**} (start/stop/pause/resume/emergency-close)는 ROLE_ADMIN 을
 * 요구해야 한다 — 크립토 {@code /api/trading/**} 선례(ADR common/security/0003, TradingApiSecurityTest).
 * 단 {@code GET /api/stock/bot/status} 는 공개 대시보드(/stock) 위젯이 사용하므로 permitAll 유지
 * (조회 API 전반의 노출 범위는 후속 과제 — ADR common/security/0005 참고).
 */
@WebMvcTest(StockBotApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class StockBotApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GapPullbackBotService botService;

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
    @DisplayName("미인증 사용자의 긴급청산 호출은 로그인으로 리다이렉트되고 서비스까지 도달하지 않는다")
    void unauthenticatedEmergencyCloseIsDenied() throws Exception {
        mockMvc.perform(post("/api/stock/bot/emergency-close"))
                .andExpect(status().is3xxRedirection());

        verify(botService, never()).emergencyCloseAll();
    }

    @Test
    @DisplayName("미인증 사용자의 봇 시작 호출은 차단되고 서비스까지 도달하지 않는다")
    void unauthenticatedStartIsDenied() throws Exception {
        mockMvc.perform(post("/api/stock/bot/start"))
                .andExpect(status().is3xxRedirection());

        verify(botService, never()).start();
    }

    @Test
    @DisplayName("비관리자(ROLE_USER)의 봇 제어 호출은 403 으로 거부된다")
    void nonAdminControlIsForbidden() throws Exception {
        mockMvc.perform(post("/api/stock/bot/stop").with(user("op").roles("USER")))
                .andExpect(status().isForbidden());

        verify(botService, never()).stop();
    }

    @Test
    @DisplayName("관리자(ROLE_ADMIN)는 봇 제어 API 에 접근할 수 있다")
    void adminCanReachControl() throws Exception {
        when(botService.start()).thenReturn(true);

        mockMvc.perform(post("/api/stock/bot/start").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(botService).start();
    }

    @Test
    @DisplayName("봇 상태 조회(GET)는 공개 대시보드 위젯을 위해 무인증 접근을 유지한다")
    void statusRemainsPubliclyReadable() throws Exception {
        when(botService.getStatus()).thenReturn(new GapPullbackBotService.BotStatus(
            false, false, false, 0, 0, "MARKET_CLOSED", null, null, 0, null));

        mockMvc.perform(get("/api/stock/bot/status"))
                .andExpect(status().isOk());
    }
}
