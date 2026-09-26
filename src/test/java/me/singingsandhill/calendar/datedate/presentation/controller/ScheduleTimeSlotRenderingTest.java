package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import me.singingsandhill.calendar.datedate.application.service.TimeSlotService;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;

/**
 * 일정 페이지·상세 API 가 시간 투표 후보를 싣는지 — 프론트(timeslots.js)는
 * window.SCHEDULE_DATA.timeSlots 만 보고 목록·겹침 차트를 그린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleTimeSlotRenderingTest {

    private static final String OWNER_ID = "timeslot-view-owner";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private EntityManager entityManager;

    private void givenVotedTimeSlotOnDay20() {
        Schedule schedule = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        Participant alice = participantService.addParticipant(schedule.getId(), "Alice");
        participantService.updateSelections(alice.getId(), List.of(20));
        // 운영은 요청마다 새 영속성 컨텍스트 — 1차 캐시의 참여자 컬렉션이 stale 하지 않도록 재현
        entityManager.flush();
        entityManager.clear();
        TimeSlot slot = timeSlotService.addTimeSlot(schedule.getId(), 20, 1080, 1200);
        timeSlotService.vote(slot.getId(), "Alice");
    }

    @Test
    @DisplayName("일정 페이지는 SCHEDULE_DATA.timeSlots 에 후보·투표자를 주입하고 시간 투표 섹션을 렌더한다")
    void schedulePage_injectsTimeSlots() throws Exception {
        givenVotedTimeSlotOnDay20();

        String html = mockMvc.perform(get("/" + OWNER_ID + "/2025/12"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String timeSlotsData = html.substring(html.indexOf("timeSlots:"), html.indexOf("messages:"));
        assertThat(timeSlotsData)
                .containsPattern("dayIndex:\\s*20")
                .containsPattern("startMinute:\\s*1080")
                .containsPattern("endMinute:\\s*1200")
                .contains("\"Alice\"");
        assertThat(html).contains("id=\"timeSlotList\"");
        assertThat(html).doesNotContain("??schedule.time");
    }

    @Test
    @DisplayName("영문 로케일에서도 시간 투표 메시지 키가 모두 해석된다")
    void schedulePage_english_noMissingKeys() throws Exception {
        givenVotedTimeSlotOnDay20();

        String html = mockMvc.perform(get("/" + OWNER_ID + "/2025/12").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("id=\"timeSlotList\"");
        assertThat(html).doesNotContain("??schedule.time");
    }

    @Test
    @DisplayName("일정 상세 API 응답에도 timeSlots 가 포함된다")
    void scheduleApi_includesTimeSlots() throws Exception {
        givenVotedTimeSlotOnDay20();

        mockMvc.perform(get("/api/owners/" + OWNER_ID + "/schedules/2025/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlots[0].dayIndex").value(20))
                .andExpect(jsonPath("$.timeSlots[0].voters[0]").value("Alice"));
    }
}
