package me.singingsandhill.calendar.trading.presentation.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import me.singingsandhill.calendar.trading.application.service.TradingBotService;

/**
 * P0-1: 트레이딩 봇 제어·실주문 REST API 가 무인증으로 노출되면 안 된다.
 * {@code /api/trading/**} 는 {@code ROLE_ADMIN} 을 요구해야 하며, 미인증/비관리자는 차단된다.
 */
@WebMvcTest(BotControlApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class TradingApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingBotService tradingBotService;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("미인증 사용자의 긴급청산 호출은 로그인으로 리다이렉트되고 서비스까지 도달하지 않는다")
    void unauthenticatedEmergencyCloseIsDenied() throws Exception {
        mockMvc.perform(post("/api/trading/bot/emergency-close"))
                .andExpect(status().is3xxRedirection());

        verify(tradingBotService, never()).emergencyClose();
    }

    @Test
    @DisplayName("비관리자(ROLE_USER)의 긴급청산 호출은 403 으로 거부되고 서비스까지 도달하지 않는다")
    void nonAdminEmergencyCloseIsForbidden() throws Exception {
        mockMvc.perform(post("/api/trading/bot/emergency-close").with(user("op").roles("USER")))
                .andExpect(status().isForbidden());

        verify(tradingBotService, never()).emergencyClose();
    }

    @Test
    @DisplayName("관리자(ROLE_ADMIN)는 긴급청산 API 에 접근할 수 있다")
    void adminCanReachEmergencyClose() throws Exception {
        mockMvc.perform(post("/api/trading/bot/emergency-close").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(tradingBotService).emergencyClose();
    }

    @Test
    @DisplayName("미인증 사용자의 트레이딩 대시보드(/trading) 접근은 로그인으로 리다이렉트된다")
    void unauthenticatedDashboardIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/trading"))
                .andExpect(status().is3xxRedirection());
    }
}
