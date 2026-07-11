package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private UserActivityRepository repository;

    private UserActivityService service;

    @BeforeEach
    void setUp() {
        service = new UserActivityService(repository, FIXED);
    }

    @Test
    @DisplayName("로그인 사용자의 활동은 1행 기록된다")
    void recordsActivityForLoggedInUser() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.LOCATION_VOTE, 5L))
                .thenReturn(false);

        service.record(42L, ActivityType.LOCATION_VOTE, 3L, 5L, "성수 카페");

        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(repository).save(captor.capture());
        UserActivity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getType()).isEqualTo(ActivityType.LOCATION_VOTE);
        assertThat(saved.getScheduleId()).isEqualTo(3L);
        assertThat(saved.getTargetId()).isEqualTo(5L);
        assertThat(saved.getDetail()).isEqualTo("성수 카페");
        assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("userId 가 null(비로그인)이면 아무것도 하지 않는다")
    void noOpForAnonymous() {
        service.record(null, ActivityType.PARTICIPATION, 3L, 5L, "지수");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("(userId, type, targetId) 중복 이벤트는 다시 기록하지 않는다")
    void skipsDuplicateByUserTypeTarget() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.PARTICIPATION, 5L))
                .thenReturn(true);

        service.record(42L, ActivityType.PARTICIPATION, 3L, 5L, "지수");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("저장 실패는 예외를 삼키고 본 동작을 막지 않는다")
    void swallowsPersistenceFailure() {
        when(repository.existsByUserIdAndTypeAndTargetId(42L, ActivityType.MENU_VOTE, 5L))
                .thenReturn(false);
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.record(42L, ActivityType.MENU_VOTE, 3L, 5L, "마라탕"))
                .doesNotThrowAnyException();
    }
}
