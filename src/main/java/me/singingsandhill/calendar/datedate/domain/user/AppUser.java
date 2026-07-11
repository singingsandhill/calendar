package me.singingsandhill.calendar.datedate.domain.user;

import java.time.LocalDateTime;

public class AppUser {

    private final Long id;
    private final Long kakaoId;
    private String nickname;
    private String profileImageUrl;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public AppUser(Long id, Long kakaoId, String nickname, String profileImageUrl,
                   LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        if (kakaoId == null) {
            throw new IllegalArgumentException("kakaoId cannot be null");
        }
        this.id = id;
        this.kakaoId = kakaoId;
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static AppUser signUp(Long kakaoId, String nickname, String profileImageUrl, LocalDateTime now) {
        return new AppUser(null, kakaoId, nickname, profileImageUrl, now, now);
    }

    public void refreshProfile(String nickname, String profileImageUrl, LocalDateTime loginAt) {
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = loginAt;
    }

    private static String normalizeNickname(String nickname) {
        return (nickname == null || nickname.isBlank()) ? "카카오사용자" : nickname;
    }

    public Long getId() {
        return id;
    }

    public Long getKakaoId() {
        return kakaoId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
