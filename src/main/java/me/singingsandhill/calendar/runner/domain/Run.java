package me.singingsandhill.calendar.runner.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Run {

    private static final int MAX_LOCATION_LENGTH = 100;

    private Long id;
    private final LocalDate date;
    private final LocalTime time;
    private final String location;
    private final RunCategory category;
    private final LocalDateTime createdAt;

    public Run(LocalDate date, LocalTime time, String location, RunCategory category) {
        this(null, date, time, location, category, LocalDateTime.now());
    }

    public Run(Long id, LocalDate date, LocalTime time, String location,
               RunCategory category, LocalDateTime createdAt) {
        validateDate(date);
        validateTime(time);
        validateLocation(location);
        validateCategory(category);
        this.id = id;
        this.date = date;
        this.time = time;
        this.location = location;
        this.category = category;
        this.createdAt = createdAt;
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
    }

    private void validateTime(LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
    }

    private void validateLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be blank");
        }
        if (location.length() > MAX_LOCATION_LENGTH) {
            throw new IllegalArgumentException("Location cannot exceed " + MAX_LOCATION_LENGTH + " characters");
        }
    }

    private void validateCategory(RunCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getLocation() {
        return location;
    }

    public RunCategory getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
