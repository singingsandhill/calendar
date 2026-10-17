package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

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
import me.singingsandhill.calendar.datedate.application.service.TimeSlotService;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlotRepository;

/**
 * 시간 투표 후보의 실제 JPA 왕복 검증 — 투표 자식 테이블 동기화, 일정 삭제 cascade.
 *
 * <p>운영에서는 요청마다 영속성 컨텍스트가 새로 열리므로, 단계 사이에 flush + clear 로
 * 같은 조건을 재현한다 (1차 캐시가 cascade 누락을 가리지 않도록).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimeSlotPersistenceIntegrationTest {

    private static final String OWNER_ID = "timeslot-it-owner";

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private EntityManager entityManager;

    private void newRequest() {
        entityManager.flush();
        entityManager.clear();
    }

    private Schedule scheduleWithAliceOnDay20() {
        Schedule schedule = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        Participant alice = participantService.addParticipant(schedule.getId(), "Alice");
        participantService.updateSelections(alice.getId(), List.of(20));
        newRequest();
        return schedule;
    }

    @Test
    @DisplayName("후보 추가·투표·취소가 저장되고 일정별 조회에 투표자와 함께 나온다")
    void addVoteUnvote_roundTrip() {
        Schedule schedule = scheduleWithAliceOnDay20();

        TimeSlot added = timeSlotService.addTimeSlot(schedule.getId(), 20, 1080, 1200);
        newRequest();
        timeSlotService.vote(added.getId(), "Alice");
        newRequest();
        timeSlotService.vote(added.getId(), "Bob");
        newRequest();
        timeSlotService.unvote(added.getId(), "bob");
        newRequest();

        List<TimeSlot> slots = timeSlotService.getTimeSlotsByScheduleId(schedule.getId());

        assertThat(slots).hasSize(1);
        TimeSlot slot = slots.get(0);
        assertThat(slot.getId()).isEqualTo(added.getId());
        assertThat(slot.getDayIndex()).isEqualTo(20);
        assertThat(slot.getStartMinute()).isEqualTo(1080);
        assertThat(slot.getEndMinute()).isEqualTo(1200);
        assertThat(slot.getVoters()).containsExactly("Alice");
    }

    @Test
    @DisplayName("같은 날짜·시간대 존재 여부는 일정 단위로 판별한다")
    void existsByScheduleIdAndRange_matchesExactRangeOnly() {
        Schedule schedule = scheduleWithAliceOnDay20();
        timeSlotService.addTimeSlot(schedule.getId(), 20, 1080, 1200);
        newRequest();

        assertThat(timeSlotRepository.existsByScheduleIdAndRange(schedule.getId(), 20, 1080, 1200)).isTrue();
        assertThat(timeSlotRepository.existsByScheduleIdAndRange(schedule.getId(), 20, 1080, 1230)).isFalse();
        assertThat(timeSlotRepository.existsByScheduleIdAndRange(schedule.getId() + 1, 20, 1080, 1200)).isFalse();
    }

    @Test
    @DisplayName("일정을 삭제하면 후보와 투표가 FK 오류 없이 함께 삭제된다")
    void deleteSchedule_cascadesToTimeSlotsAndVotes() {
        Schedule schedule = scheduleWithAliceOnDay20();
        TimeSlot added = timeSlotService.addTimeSlot(schedule.getId(), 20, 1080, 1200);
        newRequest();
        timeSlotService.vote(added.getId(), "Alice");
        newRequest();

        scheduleService.deleteSchedule(OWNER_ID, 2025, 12);
        newRequest();

        assertThat(timeSlotRepository.findById(added.getId())).isEmpty();
    }
}
