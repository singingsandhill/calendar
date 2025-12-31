package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "participants")
public class ParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleJpaEntity schedule;

    @Column(length = 10, nullable = false)
    private String name;

    @Column(length = 7, nullable = false)
    private String color;

    @Column(length = 100)
    private String selections;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ParticipantJpaEntity() {
    }

    public ParticipantJpaEntity(ScheduleJpaEntity schedule, String name, String color,
                                 String selections, LocalDateTime updatedAt) {
        this.schedule = schedule;
        this.name = name;
        this.color = color;
        this.selections = selections;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public ScheduleJpaEntity getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleJpaEntity schedule) {
        this.schedule = schedule;
    }

    public Long getScheduleId() {
        return schedule != null ? schedule.getId() : null;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getSelections() {
        return selections;
    }

    public void setSelections(String selections) {
        this.selections = selections;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
