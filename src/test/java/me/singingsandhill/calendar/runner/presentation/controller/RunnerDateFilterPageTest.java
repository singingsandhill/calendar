package me.singingsandhill.calendar.runner.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.runner.domain.Attendance;
import me.singingsandhill.calendar.runner.domain.AttendanceRepository;
import me.singingsandhill.calendar.runner.domain.Run;
import me.singingsandhill.calendar.runner.domain.RunCategory;
import me.singingsandhill.calendar.runner.domain.RunRepository;

/**
 * 런 목록(/runners/runs)·출석 현황(/runners/members)·어드민 대시보드(/runners/admin)의
 * from/to 날짜 범위 필터 검증. 잘못된 파라미터는 무시(무제한)한다 —
 * WebConfig.ignoreInvalidLocale 과 같은 "봇 스캔 500 방지" 원칙.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RunnerDateFilterPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private static final String LOCATION_A = "필터장소A";
    private static final String LOCATION_B = "필터장소B";
    private static final String RUNNER_A = "필터김러너";
    private static final String RUNNER_B = "필터이러너";

    @BeforeEach
    void seed() {
        Run runA = runRepository.save(new Run(
                LocalDate.of(2026, 1, 10), LocalTime.of(9, 0), LOCATION_A, RunCategory.REGULAR));
        Run runB = runRepository.save(new Run(
                LocalDate.of(2026, 3, 15), LocalTime.of(19, 30), LOCATION_B, RunCategory.LIGHTNING));
        attendanceRepository.save(new Attendance(runA.getId(), RUNNER_A, new BigDecimal("5.0")));
        attendanceRepository.save(new Attendance(runB.getId(), RUNNER_B, new BigDecimal("7.5")));
    }

    @Test
    @DisplayName("런 목록: from~to 범위 안의 런만 표시 + 입력값 유지")
    void runList_filtersByDateRange() throws Exception {
        String html = mockMvc.perform(get("/runners/runs")
                        .param("from", "2026-02-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_B);
        assertThat(html).doesNotContain(LOCATION_A);
        assertThat(html).contains("value=\"2026-02-01\"");
        assertThat(html).contains("value=\"2026-12-31\"");
    }

    @Test
    @DisplayName("런 목록: 파라미터 없으면 전체 표시 (기존 동작 유지)")
    void runList_noParams_showsAll() throws Exception {
        String html = mockMvc.perform(get("/runners/runs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_A);
        assertThat(html).contains(LOCATION_B);
    }

    @Test
    @DisplayName("런 목록: from 만 지정하면 그 이후만")
    void runList_fromOnly() throws Exception {
        String html = mockMvc.perform(get("/runners/runs")
                        .param("from", "2026-02-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_B);
        assertThat(html).doesNotContain(LOCATION_A);
    }

    @Test
    @DisplayName("런 목록: to 만 지정하면 그 이전만")
    void runList_toOnly() throws Exception {
        String html = mockMvc.perform(get("/runners/runs")
                        .param("to", "2026-02-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_A);
        assertThat(html).doesNotContain(LOCATION_B);
    }

    @Test
    @DisplayName("런 목록: 잘못된 날짜 파라미터는 무시하고 전체 표시 (500 없음)")
    void runList_invalidParams_ignored() throws Exception {
        String html = mockMvc.perform(get("/runners/runs")
                        .param("from", "garbage")
                        .param("to", "2026-13-99"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_A);
        assertThat(html).contains(LOCATION_B);
    }

    @Test
    @DisplayName("런 목록: from > to 면 빈 결과 + 기간 전용 빈 상태 문구")
    void runList_fromAfterTo_emptyState() throws Exception {
        String html = mockMvc.perform(get("/runners/runs")
                        .param("from", "2026-12-31")
                        .param("to", "2026-01-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain(LOCATION_A);
        assertThat(html).doesNotContain(LOCATION_B);
        assertThat(html).contains("해당 기간에 등록된 런이 없습니다");
    }

    @Test
    @DisplayName("출석 현황: 범위 안 런에 출석한 멤버만 집계")
    void memberList_filtersByRunDateRange() throws Exception {
        String html = mockMvc.perform(get("/runners/members")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(RUNNER_B);
        assertThat(html).doesNotContain(RUNNER_A);
        assertThat(html).contains("총 1명의 러너");
    }

    @Test
    @DisplayName("출석 현황: 파라미터 없으면 전체 집계 (기존 동작 유지)")
    void memberList_noParams_showsAll() throws Exception {
        String html = mockMvc.perform(get("/runners/members"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(RUNNER_A);
        assertThat(html).contains(RUNNER_B);
    }

    @Test
    @DisplayName("어드민 대시보드: from~to 범위 필터 적용")
    void adminDashboard_filtersByDateRange() throws Exception {
        String html = mockMvc.perform(get("/runners/admin")
                        .param("from", "2026-02-01")
                        .param("to", "2026-12-31")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(LOCATION_B);
        assertThat(html).doesNotContain(LOCATION_A);
    }
}
