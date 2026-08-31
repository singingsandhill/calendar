package me.singingsandhill.calendar.common.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 배포 헬스 게이트용 actuator 노출 계약 (ADR common/security/0006).
 *
 * <p>배포 스크립트는 {@code /actuator/health/deploy}(db·ping·diskSpace 그룹)를 폴링한다.
 * mail 인디케이터는 반드시 집계에서 빠져야 한다 — 켜져 있으면 헬스 조회마다
 * smtp.gmail.com 에 실접속해 게이트가 외부 SMTP 가용성에 종속된다.
 *
 * <p>보안 경계: SecurityConfig 규칙 `/*{@literal /}*{@literal /}*` permitAll 이 3세그먼트
 * actuator 경로를 삼키므로, health 화이트리스트 직후 {@code /actuator/**} denyAll 이 필수다.
 * {@code @WebMvcTest} 슬라이스에는 actuator 엔드포인트가 등록되지 않아 전체 컨텍스트로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorHealthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deployHealthGroup_isPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * 집계 헬스도 UP — mail 인디케이터가 다시 켜지면 테스트 환경에서 SMTP 접속 실패로
     * DOWN(503) 이 되므로, 이 단언이 {@code management.health.mail.enabled=false} 를 고정한다.
     */
    @Test
    void aggregateHealth_isPublicAndUp_withoutMailIndicator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorPaths_areDenied() throws Exception {
        // denyAll + 미인증 → 인증 진입점(302 /login). 200 으로 열리면 안 된다.
        int envStatus = mockMvc.perform(get("/actuator/env"))
                .andReturn().getResponse().getStatus();
        assertThat(envStatus).isNotEqualTo(200);

        int componentStatus = mockMvc.perform(get("/actuator/health/db"))
                .andReturn().getResponse().getStatus();
        assertThat(componentStatus).isNotEqualTo(200);
    }
}
