package me.singingsandhill.calendar.runner.domain;

public enum RunCategory {
    REGULAR("정규런"),
    LIGHTNING("번개런");

    private final String displayName;

    RunCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
