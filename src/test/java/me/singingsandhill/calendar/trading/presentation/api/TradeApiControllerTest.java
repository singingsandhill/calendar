package me.singingsandhill.calendar.trading.presentation.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeMode;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;

/**
 * 포지션 응답의 모드·진입 출처·MFE/MAE, 손익 요약의 모드 파라미터 (ADR trading/observability/0004).
 */
@WebMvcTest(TradeApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class TradeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeRepository tradeRepository;

    @MockitoBean
    private PositionRepository positionRepository;

    @MockitoBean
    private ProfitService profitService;

    @MockitoBean
    private TradingProperties tradingProperties;

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
        when(tradingProperties.getBot()).thenReturn(new TradingProperties.Bot());
    }

    @Test
    @DisplayName("포지션 목록은 모드·진입 출처·최대 유리/불리 폭을 내려준다")
    void positionsExposeModeEntrySourceAndExcursions() throws Exception {
        Position position = Position.open("KRW-ADA", new BigDecimal("290"), BigDecimal.TEN,
            new BigDecimal("285"), new BigDecimal("299"));
        position.setMode(TradeMode.LIVE);
        position.setEntrySource(EntrySource.REBALANCE);
        position.updateHighWaterMark(new BigDecimal("295"));
        position.updateLowWaterMark(new BigDecimal("286"));
        when(positionRepository.findByMarketOrderByOpenedAtDesc(eq("KRW-ADA"), anyInt(), anyInt()))
            .thenReturn(List.of(position));

        mockMvc.perform(get("/api/trading/positions").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mode").value("LIVE"))
            .andExpect(jsonPath("$[0].entrySource").value("REBALANCE"))
            .andExpect(jsonPath("$[0].mfePct").value(1.7241))
            .andExpect(jsonPath("$[0].maePct").value(-1.3793));
    }

    @Test
    @DisplayName("손익 요약은 기본 LIVE, ?mode= 로 다른 모드를 고른다")
    void profitSummaryHonorsModeParameter() throws Exception {
        when(profitService.getProfitSummary(TradeMode.PAPER)).thenReturn(new ProfitService.ProfitSummary(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, 0, 0.0, BigDecimal.ZERO));

        mockMvc.perform(get("/api/trading/profit/summary").param("mode", "PAPER")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());

        verify(profitService).getProfitSummary(TradeMode.PAPER);
    }
}
