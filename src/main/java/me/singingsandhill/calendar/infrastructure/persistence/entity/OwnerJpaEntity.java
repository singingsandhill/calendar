package me.singingsandhill.calendar.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "owners")
public class OwnerJpaEntity {

    @Id
    @Column(length = 20)
    private String ownerId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleJpaEntity> schedules = new ArrayList<>();

    protected OwnerJpaEntity() {
    }

    public OwnerJpaEntity(String ownerId, LocalDateTime createdAt) {
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ScheduleJpaEntity> getSchedules() {
        return schedules;
    }

    public void addSchedule(ScheduleJpaEntity schedule) {
        schedules.add(schedule);
        schedule.setOwner(this);
    }
}
