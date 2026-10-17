package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "time_slots")
public class TimeSlotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleJpaEntity schedule;

    @Column(nullable = false)
    private Integer dayIndex;

    @Column(nullable = false)
    private Integer startMinute;

    @Column(nullable = false)
    private Integer endMinute;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "timeSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSlotVoteJpaEntity> votes = new ArrayList<>();

    protected TimeSlotJpaEntity() {
    }

    public TimeSlotJpaEntity(ScheduleJpaEntity schedule, Integer dayIndex, Integer startMinute,
                             Integer endMinute, LocalDateTime createdAt) {
        this.schedule = schedule;
        this.dayIndex = dayIndex;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getScheduleId() {
        return schedule != null ? schedule.getId() : null;
    }

    public Integer getDayIndex() {
        return dayIndex;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public Integer getEndMinute() {
        return endMinute;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<TimeSlotVoteJpaEntity> getVotes() {
        return votes;
    }

    public void addVote(TimeSlotVoteJpaEntity vote) {
        votes.add(vote);
        vote.setTimeSlot(this);
    }
}
