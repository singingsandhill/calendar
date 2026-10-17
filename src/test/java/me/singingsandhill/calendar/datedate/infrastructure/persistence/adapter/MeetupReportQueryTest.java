package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

/** 리포트 읽기 쿼리 — 일정 필드와 selections 변환(SelectionListConverter)이 record 에 그대로 실린다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeetupReportQueryTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ParticipantService participantService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("findAllAvailability — 일정 id·연월·주 모드·생성 시각·고른 인덱스를 담는다")
    void availabilityCarriesScheduleFieldsAndSelections() {
        Schedule s = scheduleService.createSchedule("report-query-x1", 2026, 11, null);
        Participant p = participantService.addParticipant(s.getId(), "민수");
        participantService.updateSelections(p.getId(), List.of(3, 14));
        // 1차 캐시를 비워야 조회가 DB 행에서 엔티티를 새로 만든다 — 아니면 컨버터·매핑을 거치지 않는다
        entityManager.flush();
        entityManager.clear();

        List<ParticipantAvailability> rows = participantRepository.findAllAvailability().stream()
                .filter(r -> r.scheduleId().equals(s.getId())).toList();

        assertThat(rows).hasSize(1);
        ParticipantAvailability row = rows.get(0);
        assertThat(row.year()).isEqualTo(2026);
        assertThat(row.month()).isEqualTo(11);
        assertThat(row.weeks()).isEqualTo(7);
        // DB 왕복값은 TIMESTAMP 정밀도(마이크로초)로 잘린다 — 1ms 안에서 비교한다
        assertThat(row.scheduleCreatedAt()).isCloseTo(s.getCreatedAt(), within(1, java.time.temporal.ChronoUnit.MILLIS));
        assertThat(row.selections()).containsExactly(3, 14);
    }

    @Test
    @DisplayName("findAllCreatedAt — 일정 생성 시각 목록에 새 일정이 들어 있다")
    void createdAtListContainsSchedule() {
        Schedule s = scheduleService.createSchedule("report-query-x2", 2026, 11, null);
        // DB 왕복값은 TIMESTAMP 정밀도(마이크로초)로 잘리고 메모리 값은 JVM 정밀도라 1ms 안에서 비교한다
        assertThat(scheduleRepository.findAllCreatedAt()).anySatisfy(t ->
                assertThat(t).isCloseTo(s.getCreatedAt(), within(1, java.time.temporal.ChronoUnit.MILLIS)));
    }
}
