package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

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
@Table(name = "time_slot_votes")
public class TimeSlotVoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id")
    private TimeSlotJpaEntity timeSlot;

    @Column(length = 10, nullable = false)
    private String voterName;

    protected TimeSlotVoteJpaEntity() {
    }

    public TimeSlotVoteJpaEntity(TimeSlotJpaEntity timeSlot, String voterName) {
        this.timeSlot = timeSlot;
        this.voterName = voterName;
    }

    public Long getId() {
        return id;
    }

    public TimeSlotJpaEntity getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlotJpaEntity timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getVoterName() {
        return voterName;
    }
}
