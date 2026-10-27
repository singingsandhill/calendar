package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

/**
 * 대시보드 렌더링 가드.
 * <ul>
 *   <li>일정 카드 제목은 일정 페이지 H1(ScheduleController.formatYearMonth)과 같은 표기 — ko "2026년 10월", en "October 2026".
 *       연도는 {0,number,#} 로 천단위 그룹화("2,026")를 막는다.</li>
 *   <li>owner-dashboard.js·계정 연결 스크립트의 알림 문구는 OWNER_DATA.messages 로 로케일별 주입 — JS 하드코딩 금지.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnerDashboardRenderingTest {

    private static final String OWNER_ID = "card-title-x1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @BeforeEach
    void setUp() {
        ownerRepository.save(new Owner(OWNER_ID));
        scheduleRepository.save(new Schedule(OWNER_ID, 2026, 10));
    }

    @Test
    @DisplayName("ko 카드 제목은 '2026년 10월' (그룹화·하이픈 표기 없음)")
    void koreanCardTitle() throws Exception {
        mockMvc.perform(get("/" + OWNER_ID).param("lang", "ko"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>2026년 10월</h3>")))
                .andExpect(content().string(not(containsString("2026-10"))));
    }

    @Test
    @DisplayName("en 카드 제목은 일정 페이지와 같은 'October 2026'")
    void englishCardTitle() throws Exception {
        mockMvc.perform(get("/" + OWNER_ID).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>October 2026</h3>")));
    }

    @Test
    @DisplayName("en 대시보드는 삭제 확인·링크 복사·연결 실패 문구를 영어로 주입한다")
    void englishScriptMessages() throws Exception {
        mockMvc.perform(get("/" + OWNER_ID).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleteConfirm: \"Delete this schedule?\"")))
                .andExpect(content().string(containsString("linkCopied: \"Link copied to clipboard!\"")))
                .andExpect(content().string(containsString("link this page. Please try again in a moment.")));
    }
}
