package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShareRepository;

@ExtendWith(MockitoExtension.class)
class RecapShareServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private RecapShareRepository recapShareRepository;

    private RecapShareService recapShareService;

    @BeforeEach
    void setUp() {
        recapShareService = new RecapShareService(recapShareRepository, FIXED);
    }

    @Test
    @DisplayName("기존 공유 토큰이 있으면 그대로 반환하고 save 는 호출하지 않는다 (멱등)")
    void returnsExistingShareWithoutSaving() {
        RecapShare existing = new RecapShare(1L, 42L, 2026, "existing-token",
                LocalDateTime.of(2026, 1, 1, 0, 0));
        when(recapShareRepository.findByUserIdAndYear(42L, 2026)).thenReturn(Optional.of(existing));

        RecapShare result = recapShareService.getOrCreateShare(42L, 2026);

        assertThat(result.getToken()).isEqualTo("existing-token");
        verify(recapShareRepository, never()).save(any());
    }

    @Test
    @DisplayName("공유 토큰이 없으면 새로 생성해 저장한다")
    void createsNewShareWhenAbsent() {
        when(recapShareRepository.findByUserIdAndYear(42L, 2026)).thenReturn(Optional.empty());
        when(recapShareRepository.save(any(RecapShare.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecapShare result = recapShareService.getOrCreateShare(42L, 2026);

        verify(recapShareRepository).save(any(RecapShare.class));
        assertThat(result.getUserId()).isEqualTo(42L);
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getToken()).hasSize(36);
        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("연도 범위(2024~현재) 밖이면 InvalidRecapYearException, save 미호출")
    void rejectsOutOfRangeYear() {
        assertThatThrownBy(() -> recapShareService.getOrCreateShare(42L, 2023))
                .isInstanceOf(InvalidRecapYearException.class);
        assertThatThrownBy(() -> recapShareService.getOrCreateShare(42L, 2027))
                .isInstanceOf(InvalidRecapYearException.class);

        verify(recapShareRepository, never()).save(any());
        verify(recapShareRepository, never()).findByUserIdAndYear(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("존재하지 않는 토큰 조회는 RecapShareNotFoundException")
    void getByTokenThrowsWhenMissing() {
        when(recapShareRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recapShareService.getByToken("nope"))
                .isInstanceOf(RecapShareNotFoundException.class);
    }
}
