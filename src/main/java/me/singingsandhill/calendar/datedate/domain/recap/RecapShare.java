package me.singingsandhill.calendar.datedate.domain.recap;

import java.time.LocalDateTime;

/** 연간 recap 공개 공유 토큰. (userId, year) 당 1개, 멱등 생성. */
public class RecapShare {

    private final Long id;
    private final Long userId;
    private final int year;
    private final String token;
    private final LocalDateTime createdAt;

    public RecapShare(Long id, Long userId, int year, String token, LocalDateTime createdAt) {
        if (userId == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("userId and token are required");
        }
        this.id = id;
        this.userId = userId;
        this.year = year;
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
