package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
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
import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.common.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.datedate.application.dto.InsightsOverviewDto;
import me.singingsandhill.calendar.datedate.application.dto.PopularItemDto;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.PopularityService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;
import me.singingsandhill.calendar.datedate.infrastructure.security.KakaoOAuth2UserService;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(HomeController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private PopularityService popularityService;

    @MockitoBean
    private InsightsService insightsService;

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

    @BeforeEach
    void stubDefaults() {
        when(seoService.getHomeSeo()).thenReturn(SeoMetadata.builder().title("DateDate").build());
        when(localeLinks.href(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(popularityService.getPopularLocations()).thenReturn(List.of());
        when(popularityService.getPopularMenus()).thenReturn(List.of());
        when(insightsService.getInsightsOverview())
                .thenReturn(new InsightsOverviewDto(0, 0, 0, 0, 0, null, null));
    }

    @Test
    @DisplayName("홈은 200 과 index 뷰를 렌더한다")
    void rendersIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("시나리오 그리드는 UseCaseSlugs.ALL 의 모든 슬러그 카드를 노출한다 (그리드-슬러그 동기화 가드)")
    void scenarioGridCoversAllUseCaseSlugs() throws Exception {
        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 푸터도 전체 슬러그 링크를 렌더하므로 반드시 시나리오 섹션 범위로 한정해 검사한다
        int gridStart = html.indexOf("scenarios-grid");
        assertThat(gridStart).as("홈에 scenarios-grid 섹션이 있어야 한다").isPositive();
        String grid = html.substring(gridStart, html.indexOf("</section>", gridStart));

        for (String slug : UseCaseSlugs.ALL) {
            assertThat(grid)
                    .as("시나리오 그리드에 /use-cases/%s 카드가 있어야 한다", slug)
                    .contains("/use-cases/" + slug);
        }
    }

    @Test
    @DisplayName("통계 스트립 숫자는 천단위 구분자로 렌더된다")
    void formatsStatsWithThousandsSeparator() throws Exception {
        when(insightsService.getInsightsOverview())
                .thenReturn(new InsightsOverviewDto(12345, 678, 0, 0, 9012, null, null));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12,345")))
                .andExpect(content().string(containsString("9,012")));
    }

    @Test
    @DisplayName("인기 장소/메뉴가 모두 비면 popular 섹션을 렌더하지 않는다")
    void hidesPopularSectionWhenEmpty() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("popular-section"))));
    }

    @Test
    @DisplayName("인기 항목이 있으면 popular 섹션에 이름이 렌더된다")
    void rendersPopularSectionWithItems() throws Exception {
        when(popularityService.getPopularLocations()).thenReturn(List.of(
                new PopularItemDto("강남역", null, 5, LocalDateTime.of(2026, 7, 1, 12, 0))));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("popular-section")))
                .andExpect(content().string(containsString("강남역")));
    }
}
