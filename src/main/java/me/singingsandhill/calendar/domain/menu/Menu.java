package me.singingsandhill.calendar.domain.menu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Menu {

    private static final int MAX_NAME_LENGTH = 100;

    private Long id;
    private final Long scheduleId;
    private final String name;
    private final List<String> voters;
    private final LocalDateTime createdAt;

    public Menu(Long scheduleId, String name) {
        this(null, scheduleId, name, new ArrayList<>(), LocalDateTime.now());
    }

    public Menu(Long id, Long scheduleId, String name, List<String> voters, LocalDateTime createdAt) {
        validateName(name);
        this.id = id;
        this.scheduleId = scheduleId;
        this.name = name;
        this.voters = new ArrayList<>(voters);
        this.createdAt = createdAt;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Menu name cannot be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Menu name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public String getName() {
        return name;
    }

    public List<String> getVoters() {
        return Collections.unmodifiableList(voters);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVoteCount() {
        return voters.size();
    }

    public boolean hasVoter(String voterName) {
        return voters.stream()
                .anyMatch(v -> v.equalsIgnoreCase(voterName));
    }

    public void addVote(String voterName) {
        if (voterName == null || voterName.isBlank()) {
            throw new IllegalArgumentException("Voter name cannot be blank");
        }
        if (hasVoter(voterName)) {
            throw new IllegalStateException("Already voted for this menu");
        }
        voters.add(voterName);
    }

    public void removeVote(String voterName) {
        voters.removeIf(v -> v.equalsIgnoreCase(voterName));
    }
}
