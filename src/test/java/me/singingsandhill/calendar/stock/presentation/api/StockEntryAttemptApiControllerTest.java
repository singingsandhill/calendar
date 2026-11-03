package me.singingsandhill.calendar.stock.presentation.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.domain.stock.EntryOutcome;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;

/**
 * {@code GET /api/stock/entry-attempts?date=} — 검증 단계(EntryAttempt)와 주문 단계 실패를 한 응답으로
 * (ADR stock/infrastructure/0009). 0체결 원인을 로그가 아니라 DB 로 확인하기 위한 조회 API.
 */
@WebMvcTest(StockEntryAttemptApiController.class)
@Import({CorsConfig.class, SecurityConfig.class})
class StockEntryAttemptApiControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 21);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntryAttemptRepository entryAttemptRepository;

    @MockitoBean
    private StockRepository stockRepository;

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

    private Stock failedTwice() {
        Stock stock = new Stock("000660", "SK하이닉스", DATE);
        stock.setOpenPrice(new BigDecimal("200000"));
        stock.setCurrentPrice(new BigDecimal("205000"));
        stock.recordHighFormed(new BigDecimal("207000"));
        stock.recordPullbackStart(new BigDecimal("203000"));
        stock.markEntryReady();
        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, LocalDateTime.of(2026, 10, 21, 9, 40), 3);
        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, LocalDateTime.of(2026, 10, 21, 9, 41), 3);
        return stock;
    }

    @Test
    @DisplayName("검증 단계 시도와 주문 단계 실패(사유·횟수·시각)를 함께 내려준다")
    void combinesValidationAttemptsAndOrderFailures() throws Exception {
        when(entryAttemptRepository.findByTradingDate(DATE)).thenReturn(List.of(
            EntryAttempt.of(DATE, "000660", true, 2, 2, true, true, false,
                new BigDecimal("205000"), new BigDecimal("203000"), null)));
        Stock untouched = new Stock("005930", "삼성전자", DATE);
        when(stockRepository.findByTradingDate(DATE)).thenReturn(List.of(failedTwice(), untouched));

        mockMvc.perform(get("/api/stock/entry-attempts").param("date", "2026-10-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value("2026-10-21"))
            .andExpect(jsonPath("$.validation.length()").value(1))
            .andExpect(jsonPath("$.validation[0].stockCode").value("000660"))
            .andExpect(jsonPath("$.validation[0].accepted").value(true))
            .andExpect(jsonPath("$.orderFailures.length()").value(1))
            .andExpect(jsonPath("$.orderFailures[0].stockCode").value("000660"))
            .andExpect(jsonPath("$.orderFailures[0].failCount").value(2))
            .andExpect(jsonPath("$.orderFailures[0].lastReason").value("SIZE_ZERO"))
            .andExpect(jsonPath("$.orderFailures[0].state").value("ENTRY_READY"));
    }
}
