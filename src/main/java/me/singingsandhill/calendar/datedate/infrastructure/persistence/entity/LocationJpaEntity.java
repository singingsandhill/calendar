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
@Table(name = "locations")
public class LocationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleJpaEntity schedule;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LocationVoteJpaEntity> votes = new ArrayList<>();

    protected LocationJpaEntity() {
    }

    public LocationJpaEntity(ScheduleJpaEntity schedule, String name, LocalDateTime createdAt) {
        this.schedule = schedule;
        this.name = name;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<LocationVoteJpaEntity> getVotes() {
        return votes;
    }

    public void addVote(LocationVoteJpaEntity vote) {
        votes.add(vote);
        vote.setLocation(this);
    }

    public void removeVoteByVoterName(String voterName) {
        votes.removeIf(v -> v.getVoterName().equalsIgnoreCase(voterName));
    }
}
