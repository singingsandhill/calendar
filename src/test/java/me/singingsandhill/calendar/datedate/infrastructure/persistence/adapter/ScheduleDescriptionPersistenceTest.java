package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

/**
 * 일정 설명의 JPA 왕복 — 저장한 설명이 세 조회 경로(id·연월·owner 의 일정 목록) 모두에서 그대로 읽히는지
 * (ADR datedate/domain/0011). 단계 사이 flush + clear 로 요청마다 새 영속성 컨텍스트를 재현한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleDescriptionPersistenceTest {

    private static final String OWNER_ID = "description-it-owner";
    private static final String DESCRIPTION = "회의 60분\n안건: https://notion.so/agenda";

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private EntityManager entityManager;

    private void newRequest() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("저장한 설명이 id·연월·owner 일정 목록 조회 모두에서 유지된다")
    void savedDescription_survivesAllReadPaths() {
        Schedule created = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        newRequest();
        Schedule schedule = scheduleRepository.findById(created.getId()).orElseThrow();
        schedule.changeDescription(DESCRIPTION);
        scheduleRepository.save(schedule);
        newRequest();

        assertThat(scheduleRepository.findById(created.getId()).orElseThrow().getDescription())
                .isEqualTo(DESCRIPTION);
        assertThat(scheduleRepository.findByOwnerIdAndYearMonth(OWNER_ID, 2025, 12).orElseThrow().getDescription())
                .isEqualTo(DESCRIPTION);
        assertThat(ownerRepository.findById(OWNER_ID).orElseThrow().getSchedules())
                .extracting(Schedule::getDescription)
                .containsExactly(DESCRIPTION);
    }

    @Test
    @DisplayName("설명을 지우면 null 로 저장된다")
    void clearedDescription_isStoredAsNull() {
        Schedule created = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        Schedule schedule = scheduleRepository.findById(created.getId()).orElseThrow();
        schedule.changeDescription(DESCRIPTION);
        scheduleRepository.save(schedule);
        newRequest();

        Schedule reloaded = scheduleRepository.findById(created.getId()).orElseThrow();
        reloaded.changeDescription("");
        scheduleRepository.save(reloaded);
        newRequest();

        assertThat(scheduleRepository.findById(created.getId()).orElseThrow().getDescription()).isNull();
    }
}
