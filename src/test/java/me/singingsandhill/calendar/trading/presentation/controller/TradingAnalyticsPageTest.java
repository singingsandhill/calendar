package me.singingsandhill.calendar.trading.presentation.controller;

import me.singingsandhill.calendar.common.infrastructure.config.CorsConfig;
import me.singingsandhill.calendar.common.infrastructure.config.SecurityConfig;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import me.singingsandhill.calendar.trading.application.dto.AnalyticsReport;
import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.application.service.TradingAnalyticsService;
import me.singingsandhill.calendar.trading.application.service.TradingBotService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 분석 페이지의 접근 통제와 구간 클램프.
 *
 * <p>이 페이지는 실거래 파라미터 판단 근거를 보여주므로 {@code /trading/**} 의 {@code ROLE_ADMIN}
 * 규칙 안에 있어야 한다 (SecurityConfig 규칙 #1 — 포괄 permitAll 보다 앞에 선언). 이 테스트는
 * 나중에 누가 그 순서를 건드리면 빌드가 깨지게 한다.
 */
@WebMvcTest(TradingDashboardController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class TradingAnalyticsPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingAnalyticsService analyticsService;

    @MockitoBean
    private TradingBotService tradingBotService;

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
        when(tradingProperties.getThresholds()).thenReturn(new TradingProperties.Thresholds());
        when(analyticsService.analyze(anyInt())).thenReturn(emptyReport());
    }

    private AnalyticsReport emptyReport() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 12, 0);
        AnalyticsReport.Coverage coverage = new AnalyticsReport.Coverage(
                0, 1, BigDecimal.ZERO, null, null, 0, 0, 0, Map.of(),
                0, 0, 0, 0, 0, 0, 0, false, false, List.of("신호 0행"));
        return new AnalyticsReport("KRW-ADA",
                new AnalyticsReport.Window(now.minusDays(30), now, 30, List.of(15, 60)),
                coverage, List.of(), List.of(), List.of(), List.of(),
                new AnalyticsReport.EntryContext(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of()),
                new AnalyticsReport.CostReality(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, null, null, null, new BigDecimal("0.50")),
                List.of("테스트 고지"), 1L);
    }

    @Test
    @DisplayName("미인증 사용자는 분석 페이지에 접근할 수 없다")
    void unauthenticatedAnalyticsIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/trading/analytics"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 분석 페이지에서 차단된다")
    void nonAdminAnalyticsIsForbidden() throws Exception {
        mockMvc.perform(get("/trading/analytics").with(user("op").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 빈 리포트여도 페이지를 정상 렌더한다")
    void adminReachesAnalyticsViewEvenWithEmptyReport() throws Exception {
        mockMvc.perform(get("/trading/analytics").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/analytics"))
                .andExpect(model().attributeExists("report", "market", "thresholds", "windowOptions", "selectedDays"));
    }

    @Test
    @DisplayName("데이터가 있으면 7개 섹션 표가 실제로 렌더된다")
    void adminRendersAllSectionsWhenDataExists() throws Exception {
        // 빈 리포트만 렌더하면 th:unless 로 감싼 본문 표 6개가 통째로 건너뛰어져,
        // 표 안의 Thymeleaf 표현식 오류를 잡지 못한다. 채운 리포트로 전체를 통과시킨다.
        when(analyticsService.analyze(anyInt())).thenReturn(populatedReport());

        String html = mockMvc.perform(get("/trading/analytics").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("점수 구간별 전방수익")
                .contains("구성요소별 조건부 엣지")
                .contains("임계 반사실")
                .contains("청산사유별 실현 성과")
                .contains("진입 맥락")
                .contains("비용 현실")
                .contains("40 ~ 60")            // 점수 구간 라벨
                .contains("STOP_LOSS")          // 청산 사유
                .contains("거래량 다이버전스"); // 구성요소 표시명
    }

    private AnalyticsReport populatedReport() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 12, 0);
        Map<Integer, AnalyticsReport.HorizonStat> horizons = Map.of(
                15, new AnalyticsReport.HorizonStat(15, 600, 40, new BigDecimal("0.12"),
                        new BigDecimal("0.10"), new BigDecimal("52.00"), new BigDecimal("-0.38")),
                60, new AnalyticsReport.HorizonStat(60, 600, 10, new BigDecimal("0.40"),
                        new BigDecimal("0.35"), new BigDecimal("55.00"), new BigDecimal("-0.10")));

        AnalyticsReport.Coverage coverage = new AnalyticsReport.Coverage(
                20_000, 21_600, new BigDecimal("92.59"), now.minusDays(15), now,
                15, 3, 12, Map.of(15, 19_000L, 60, 18_000L),
                120, 90, 19_790, 20_000, 20_000, 20_000, 40,
                true, true, List.of());

        AnalyticsReport.ComponentStateStat state = new AnalyticsReport.ComponentStateStat(
                "양수", 5_000, 42, new BigDecimal("12.50"), horizons);

        return new AnalyticsReport("KRW-ADA",
                new AnalyticsReport.Window(now.minusDays(15), now, 15, List.of(15, 60)),
                coverage,
                List.of(new AnalyticsReport.ScoreBucketRow("40 ~ 60", 40, 60, 5_000, 42,
                        horizons, true, false)),
                List.of(new AnalyticsReport.ComponentEdgeRow("VOLUME_DIVERGENCE", "거래량 다이버전스", 20,
                        state, state, state, state)),
                List.of(new AnalyticsReport.ThresholdScenarioRow(40, 5_000, 3_000, 3_400, 400, 30,
                        800, 400, 200, 100, horizons, true)),
                List.of(new AnalyticsReport.CloseReasonRow("STOP_LOSS", 12, new BigDecimal("-1.40"),
                        new BigDecimal("-1.45"), new BigDecimal("-16800"), new BigDecimal("300"),
                        new BigDecimal("42.0"), 0, new BigDecimal("0.00"))),
                new AnalyticsReport.EntryContext(40, 35, 5, 20, 10, 2, 3, 28,
                        List.of(new AnalyticsReport.EntryBucketRow("40 ~ 60", 20, 9, new BigDecimal("45.00"),
                                new BigDecimal("-0.30"), new BigDecimal("-0.20"), new BigDecimal("-6000"),
                                new BigDecimal("38.0"))),
                        List.of(), List.of()),
                new AnalyticsReport.CostReality(40, new BigDecimal("2000000"), new BigDecimal("-4000"),
                        new BigDecimal("10000"), new BigDecimal("-14000"), new BigDecimal("0.5000"),
                        null, new BigDecimal("250"), new BigDecimal("0.50")),
                List.of("전방수익은 gross 값이다."), 42L);
    }

    @Test
    @DisplayName("과도한 days 파라미터는 상한으로 클램프된다")
    void daysParamIsClampedToMaxWindow() throws Exception {
        mockMvc.perform(get("/trading/analytics").param("days", "9999").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedDays", TradingAnalyticsService.clampDays(9999)));

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(analyticsService).analyze(captor.capture());
        // 서비스가 안에서 자를 것을 신뢰하지 않고, 화면에 표시되는 값이 실제 구간과 일치하는지 본다.
        assertThat(TradingAnalyticsService.clampDays(captor.getValue()))
                .isEqualTo(TradingAnalyticsService.clampDays(9999));
    }
}
