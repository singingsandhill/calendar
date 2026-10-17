package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.LeadTimeBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthlyBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.WeekdayBlock;
import me.singingsandhill.calendar.datedate.application.dto.ServiceStatsDto;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;

/** /insights/trends 모임 데이터 섹션 — 블록이 있으면 그리고 null 이면 숨긴다 (ADR datedate/domain/0013). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsightsMeetupReportRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsightsService insightsService;

    private static final MeetupReportDto FULL = new MeetupReportDto(LocalDate.of(2026, 10, 14),
            new WeekdayBlock(18240, List.of(new Share("SUNDAY", 20), new Share("MONDAY", 0), new Share("TUESDAY", 0),
                    new Share("WEDNESDAY", 0), new Share("THURSDAY", 20), new Share("FRIDAY", 20), new Share("SATURDAY", 40))),
            new MonthlyBlock(30, List.of(new MonthCount(2026, 9, 10, 50, false), new MonthCount(2026, 10, 20, 100, true))),
            new LeadTimeBlock(30, 9, List.of(new Share("D0_6", 34), new Share("D7_13", 33),
                    new Share("D14_27", 20), new Share("D28_PLUS", 13))));

    @BeforeEach
    void stats() {
        when(insightsService.getServiceStats()).thenReturn(new ServiceStatsDto(0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("ko — 세 블록과 기준일·정의가 그려진다")
    void koreanBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(FULL);
        String text = visibleText(render("ko"));
        assertThat(text).contains("기준: 2026-10-14").contains("요일별 가능 비율").contains("날짜 18,240개").contains("토요일 40%")
                .contains("월별 일정 수").contains("2026년 10월").contains("진행 중")
                .contains("미리 잡는 기간").contains("중앙값 9일").contains("6일 이내 34%");
    }

    @Test
    @DisplayName("en — 영어로 그려진다")
    void englishBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(FULL);
        String text = visibleText(render("en"));
        assertThat(text).contains("As of 2026-10-14").contains("Availability by day of week").contains("Saturday 40%")
                .contains("Schedules per month").contains("in progress").contains("Median 9 days");
    }

    @Test
    @DisplayName("en — 중앙값 1일은 단수(1 day)")
    void englishMedianSingular() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(new MeetupReportDto(LocalDate.of(2026, 10, 14), null, null,
                new LeadTimeBlock(30, 1, List.of(new Share("D0_6", 100), new Share("D7_13", 0),
                        new Share("D14_27", 0), new Share("D28_PLUS", 0)))));
        assertThat(visibleText(render("en"))).contains("Median 1 day").doesNotContain("Median 1 days");
        assertThat(visibleText(render("ko"))).contains("중앙값 1일");
    }

    @Test
    @DisplayName("블록이 모두 null 이면 모임 데이터 섹션이 없다")
    void hiddenWhenNoBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(new MeetupReportDto(LocalDate.of(2026, 10, 14), null, null, null));
        String text = visibleText(render("ko"));
        assertThat(text).doesNotContain("요일별 가능 비율").doesNotContain("기준: 2026-10-14");
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get("/insights/trends").param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")).replaceAll("\\s+", " ");
    }
}
