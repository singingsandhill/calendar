package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

/**
 * 일정 설명의 끝단 — PATCH 로 저장한 설명이 일정 페이지에 이스케이프된 평문 + http(s) 링크로만 그려지고,
 * 상세 API 가 원문·조각을 내려주는지 (ADR datedate/domain/0011).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleDescriptionRenderingTest {

    private static final String OWNER_ID = "desc-view-owner";
    private static final String PAGE = "/" + OWNER_ID + "/2025/12";
    private static final String API = "/api/owners/" + OWNER_ID + "/schedules/2025/12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private EntityManager entityManager;

    private void givenSchedule() {
        scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        entityManager.flush();
        entityManager.clear();
    }

    private void patchDescription(String json) throws Exception {
        mockMvc.perform(patch(API).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get(PAGE).param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String section(String html) {
        int start = html.indexOf("id=\"scheduleDescription\"");
        return html.substring(start, html.indexOf("</section>", start));
    }

    @Test
    @DisplayName("저장한 설명은 평문은 이스케이프되고 http(s) 만 새 탭 링크로 그려진다")
    void savedDescription_rendersEscapedTextAndSafeLinks() throws Exception {
        givenSchedule();
        patchDescription("{\"description\": \"안건 https://notion.so/agenda 확인\\n<script>alert(1)</script> javascript:alert(2)\"}");

        String section = section(render("ko"));

        assertThat(section)
                .contains("<a href=\"https://notion.so/agenda\" target=\"_blank\" rel=\"nofollow ugc noopener noreferrer\">https://notion.so/agenda</a>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)")
                .doesNotContain("href=\"javascript:");
        assertThat(section).contains("data-action=\"description.edit\"");
    }

    @Test
    @DisplayName("편집 상자는 현재 원문으로 채워진다")
    void editor_prefilledWithRawDescription() throws Exception {
        givenSchedule();
        patchDescription("{\"description\": \"회의 60분\\n장소 미정\"}");

        String section = section(render("ko"));

        assertThat(section).contains("id=\"descriptionInput\"").contains("회의 60분\n장소 미정</textarea>");
        // 템플릿 maxlength 가 도메인 한도와 어긋나면 화면은 막는데 서버는 받거나 그 반대가 된다
        assertThat(section).contains("maxlength=\"" + Schedule.MAX_DESCRIPTION_LENGTH + "\"");
    }

    @Test
    @DisplayName("설명이 없으면 본문 없이 '+ 설명 추가' 버튼만 보인다")
    void noDescription_showsAddButtonOnly() throws Exception {
        givenSchedule();

        String section = section(render("ko"));

        assertThat(section).contains("+ 설명 추가").doesNotContain("schedule-description-body");
    }

    @Test
    @DisplayName("영문 로케일에서도 설명 메시지 키가 모두 해석된다")
    void english_noMissingKeys() throws Exception {
        givenSchedule();
        patchDescription("{\"description\": \"Agenda https://notion.so/a\"}");

        String html = render("en");

        assertThat(section(html)).contains("Edit");
        assertThat(html).doesNotContain("??schedule.description");
    }

    @Test
    @DisplayName("상세 API 는 원문과 링크 조각을 내려주고, 빈 문자열 PATCH 는 설명을 지운다")
    void api_exposesDescriptionAndSegments_andEmptyClears() throws Exception {
        givenSchedule();
        patchDescription("{\"description\": \"안건 https://notion.so/agenda\"}");

        mockMvc.perform(get(API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("안건 https://notion.so/agenda"))
                .andExpect(jsonPath("$.descriptionSegments[0].text").value("안건 "))
                .andExpect(jsonPath("$.descriptionSegments[0].url").value(nullValue()))
                .andExpect(jsonPath("$.descriptionSegments[1].url").value("https://notion.so/agenda"));

        patchDescription("{\"description\": \"\"}");

        mockMvc.perform(get(API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.descriptionSegments").isEmpty());
    }
}
