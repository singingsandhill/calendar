package me.singingsandhill.calendar.common.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 잘못된 {@code ?lang=} 값 방어 회귀 테스트.
 *
 * 봇 스캔 트래픽(`?lang=../../../../tmp/...`)이 {@code LocaleChangeInterceptor} 의
 * {@code parseLocale} 에서 IllegalArgumentException 을 일으켜 요청마다 500 +
 * ERROR 스택트레이스를 만들었다 (2026-07-27 운영 로그). 외부 입력이 고를 수 있는
 * 값은 5xx 가 아니라 무시(기본 로케일 폴백)여야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocaleChangeInvalidLangTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("경로조작형 lang 값은 500 이 아니라 무시하고 기본 로케일(ko)로 정상 렌더")
    void invalidLangParam_doesNotCause500() throws Exception {
        mockMvc.perform(get("/privacy").param("lang", "../../../../../../../../tmp/index1"))
                .andExpect(status().isOk())
                // "무시" 계약: 기본 로케일 폴백 + 잘못된 값이 쿠키로 저장되지 않음
                .andExpect(header().string("Content-Language", "ko"))
                .andExpect(cookie().doesNotExist("lang"));
    }

    @Test
    @DisplayName("유효한 lang=en 은 계속 적용됨 (회귀 짝)")
    void validLang_stillApplied() throws Exception {
        mockMvc.perform(get("/privacy").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en"));
    }
}
