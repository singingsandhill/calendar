package me.singingsandhill.calendar.stock.presentation.controller;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주식 대시보드 페이지의 공용 크롬 가드.
 *
 * <p>토스트 컨테이너가 {@code stock/fragments/header :: navbar} 밖에 있으면 페이지에 포함되지 않아
 * 봇 start/stop·긴급청산 결과 토스트가 전부 조용히 버려진다. 이 테스트는 fragment 경계를 고정한다.
 */
@WebMvcTest(StockDashboardController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class StockDashboardPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GapPullbackBotService botService;

    @MockitoBean
    private ScreeningService screeningService;

    @MockitoBean
    private StockPositionService positionService;

    @MockitoBean
    private StockProperties stockProperties;

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

    @BeforeEach
    void setUp() {
        when(botService.getStatus()).thenReturn(new GapPullbackBotService.BotStatus(
                false, false, false, 0, 0, "IDLE", null, null, 0, null));
    }

    @Test
    @DisplayName("공용 navbar fragment 가 토스트 컨테이너를 함께 렌더한다")
    void navbarFragmentIncludesToastContainer() throws Exception {
        String html = mockMvc.perform(get("/stock"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("id=\"toast-container\"");
    }
}
