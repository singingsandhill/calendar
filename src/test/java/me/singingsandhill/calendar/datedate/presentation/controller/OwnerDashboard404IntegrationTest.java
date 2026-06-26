package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

/**
 * GET /{ownerId} 의 무변형 + 소프트 404 제거 검증 (ADR datedate/domain/0004).
 *
 * <p>임의 URL (예: /zz-no-such-page) 이 200 + Owner row 영속화로 응답하면
 * 크롤러가 무한 soft-404 를 보고 DB 가 봇 트래픽으로 오염된다. 미존재 owner 는
 * 동일한 빈 상태 대시보드를 404 상태로 렌더링해야 하고, owner 생성은
 * POST /start 와 schedule 생성 경로로 한정된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnerDashboard404IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("미존재 owner GET → 404 + dashboard 뷰 렌더 + Owner row 미생성")
    void unknownOwner_404DashboardView_noPersistence() throws Exception {
        mockMvc.perform(get("/zz-no-such-page"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("owner/dashboard"));

        assertThat(ownerRepository.existsById("zz-no-such-page")).isFalse();
    }

    @Test
    @DisplayName("존재하는 owner GET → 200 + dashboard 뷰")
    void existingOwner_200() throws Exception {
        ownerRepository.save(new Owner("smoke-owner-x1"));

        mockMvc.perform(get("/smoke-owner-x1"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/dashboard"));
    }
}
