package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.converter.SelectionListConverter;

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

    @Convert(converter = SelectionListConverter.class)
    @Column(length = 500)
    private List<Integer> selections;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ParticipantJpaEntity() {
    }

    public ParticipantJpaEntity(ScheduleJpaEntity schedule, String name, String color,
                                 List<Integer> selections, LocalDateTime updatedAt) {
        this.schedule = schedule;
        this.name = name;
        this.color = color;
        this.selections = selections != null ? new ArrayList<>(selections) : new ArrayList<>();
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

    public List<Integer> getSelections() {
        return selections != null ? selections : new ArrayList<>();
    }

    public void setSelections(List<Integer> selections) {
        this.selections = selections != null ? new ArrayList<>(selections) : new ArrayList<>();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
