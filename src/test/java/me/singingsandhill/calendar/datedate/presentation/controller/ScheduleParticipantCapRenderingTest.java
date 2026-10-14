package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

/**
 * 참여자 추가 모달의 한도 표시 — 버튼 비활성·카운터가 서버 한도(ScheduleDetailResponse.maxParticipants)를
 * 따르는지. 템플릿에 숫자를 적어 두면 한도를 바꿀 때 서버는 받는데 웹 버튼만 옛 한도에서 잠긴다
 * (ADR datedate/domain/0010).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleParticipantCapRenderingTest {

    private static final String OWNER_ID = "cap-view-owner";
    private static final int MAX = Schedule.MAX_PARTICIPANTS;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private EntityManager entityManager;

    private void givenParticipants(int count) {
        Schedule schedule = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        for (int i = 0; i < count; i++) {
            participantService.addParticipant(schedule.getId(), "p" + i);
            // 운영은 요청마다 새 영속성 컨텍스트 — 1차 캐시의 참여자 컬렉션이 stale 하지 않도록 재현
            entityManager.flush();
            entityManager.clear();
        }
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get("/" + OWNER_ID + "/2025/12").param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String addForm(String html) {
        int start = html.indexOf("id=\"addParticipantForm\"");
        return html.substring(start, html.indexOf("</form>", start));
    }

    private static String counter(String html) {
        int start = html.indexOf("class=\"participant-count-info\"");
        int open = html.indexOf('>', start) + 1;
        return html.substring(open, html.indexOf("</p>", open)).strip();
    }

    @Test
    @DisplayName("한도 직전(MAX-1명)에는 추가 버튼이 활성이고 카운터가 서버 한도를 보여준다")
    void belowCap_addButtonEnabled() throws Exception {
        givenParticipants(MAX - 1);

        String html = render("ko");

        assertThat(addForm(html)).doesNotContain("disabled");
        assertThat(counter(html)).isEqualTo((MAX - 1) + " / " + MAX + "명");
    }

    @Test
    @DisplayName("한도(MAX명)에 도달하면 추가 버튼이 비활성이다")
    void atCap_addButtonDisabled() throws Exception {
        givenParticipants(MAX);

        String html = render("ko");

        assertThat(addForm(html)).contains("disabled");
        assertThat(counter(html)).isEqualTo(MAX + " / " + MAX + "명");
    }

    @Test
    @DisplayName("영문 로케일 카운터도 서버 한도를 쓰고 메시지 키 누락이 없다")
    void english_counterUsesCap() throws Exception {
        givenParticipants(MAX);

        String html = render("en");

        assertThat(counter(html)).isEqualTo(MAX + " / " + MAX);
        assertThat(html).doesNotContain("??schedule.participant");
    }

    @Test
    @DisplayName("일정 상세 API 가 maxParticipants 를 내려준다 — 미니앱이 한도를 하드코딩하지 않게")
    void scheduleApi_exposesMaxParticipants() throws Exception {
        givenParticipants(1);

        mockMvc.perform(get("/api/owners/" + OWNER_ID + "/schedules/2025/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipants").value(MAX));
    }
}
