package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), SEOUL);

    @Mock
    private AppUserRepository appUserRepository;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository, FIXED);
    }

    @Test
    @DisplayName("신규 카카오 사용자는 가입 처리되고 lastLoginAt 이 현재 시각이다")
    void upsertCreatesNewUser() {
        when(appUserRepository.findByKakaoId(12345L)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = appUserService.upsertKakaoUser(12345L, "지수", "https://img.example/p.jpg");

        assertThat(user.getKakaoId()).isEqualTo(12345L);
        assertThat(user.getNickname()).isEqualTo("지수");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://img.example/p.jpg");
        assertThat(user.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED));
        assertThat(user.getLastLoginAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("기존 사용자는 재로그인 시 닉네임·프로필이 갱신되고 lastLoginAt 이 갱신된다")
    void upsertRefreshesExistingUser() {
        AppUser existing = new AppUser(7L, 12345L, "옛닉", null,
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 1, 0, 0));
        when(appUserRepository.findByKakaoId(12345L)).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = appUserService.upsertKakaoUser(12345L, "새닉", "https://img.example/new.jpg");

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getNickname()).isEqualTo("새닉");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://img.example/new.jpg");
        assertThat(user.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(user.getLastLoginAt()).isEqualTo(LocalDateTime.now(FIXED));
    }

    @Test
    @DisplayName("미존재 사용자 조회는 UserNotFoundException(404)")
    void getUserThrowsWhenMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.getUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
