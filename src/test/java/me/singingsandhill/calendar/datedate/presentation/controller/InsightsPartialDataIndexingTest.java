package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * /insights/trends 부분 데이터 창 봉합 검증 (AdSense 저가치 콘텐츠 진단 2026-08-17).
 *
 * <p>일정만 존재하고 인기 장소/메뉴가 하나도 없는 상태(부분 데이터)에서는 페이지 본문이
 * 0 값 통계 카드 + 빈 상태 메시지뿐이라 색인 가치가 없다. 이 상태의 색인/광고 조건을
 * 사이트맵 등재 조건({@code SitemapService.computeInsightsLastmodIfPresent} — 장소/메뉴
 * 활동만 인정)과 일치시켜, noindex 로 강등돼야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InsightsPartialDataIndexingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("일정만 있고 인기 장소/메뉴가 없으면 → noindex, follow (사이트맵 조건과 일치)")
    void schedulesOnlyWithoutPopularItems_noindex() throws Exception {
        ownerRepository.save(new Owner("insights-partial-x1"));
        scheduleRepository.save(new Schedule("insights-partial-x1", 2026, 8));

        String html = mockMvc.perform(get("/insights/trends"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("content=\"noindex, follow\"");
    }

    @Test
    @DisplayName("데이터가 전혀 없으면 → noindex, follow (기존 빈 데이터 가드 유지)")
    void emptyData_noindex() throws Exception {
        String html = mockMvc.perform(get("/insights/trends"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("content=\"noindex, follow\"");
    }
}
