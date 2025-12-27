package me.singingsandhill.calendar.domain.runner;

import java.time.LocalDateTime;

public class Admin {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;

    private Long id;
    private final String username;
    private final String password;
    private final LocalDateTime createdAt;

    public Admin(String username, String password) {
        this(null, username, password, LocalDateTime.now());
    }

    public Admin(Long id, String username, String password, LocalDateTime createdAt) {
        validateUsername(username);
        validatePassword(password);
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
