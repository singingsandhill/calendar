package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;

/**
 * 로그인 사용자 활동 이벤트 기록. 컨트롤러가 본 동작 성공 후 호출한다.
 * 기록 실패가 참여/투표 자체를 실패시키면 안 되므로 REQUIRES_NEW + 예외 삼킴.
 */
@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserActivityRepository repository;
    private final Clock clock;

    public UserActivityService(UserActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, ActivityType type, Long scheduleId, Long targetId, String detail) {
        if (userId == null) {
            return;
        }
        try {
            if (targetId != null && repository.existsByUserIdAndTypeAndTargetId(userId, type, targetId)) {
                return;
            }
            repository.save(new UserActivity(null, userId, type, scheduleId, targetId, detail,
                    LocalDateTime.now(clock)));
        } catch (Exception e) {
            log.warn("user activity record failed: userId={}, type={}, targetId={}", userId, type, targetId, e);
        }
    }
}
