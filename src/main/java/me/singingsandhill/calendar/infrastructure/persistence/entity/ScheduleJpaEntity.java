package me.singingsandhill.calendar.infrastructure.persistence.entity;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "`year`", "`month`"}))
public class ScheduleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private OwnerJpaEntity owner;

    @Column(name = "`year`", nullable = false)
    private Integer year;

    @Column(name = "`month`", nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer weeks;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipantJpaEntity> participants = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LocationJpaEntity> locations = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuJpaEntity> menus = new ArrayList<>();

    protected ScheduleJpaEntity() {
    }

    public ScheduleJpaEntity(OwnerJpaEntity owner, Integer year, Integer month, Integer weeks, LocalDateTime createdAt) {
        this.owner = owner;
        this.year = year;
        this.month = month;
        this.weeks = weeks;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public OwnerJpaEntity getOwner() {
        return owner;
    }

    public void setOwner(OwnerJpaEntity owner) {
        this.owner = owner;
    }

    public String getOwnerId() {
        return owner != null ? owner.getOwnerId() : null;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getWeeks() {
        return weeks;
    }

    public void setWeeks(Integer weeks) {
        this.weeks = weeks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ParticipantJpaEntity> getParticipants() {
        return participants;
    }

    public void addParticipant(ParticipantJpaEntity participant) {
        participants.add(participant);
        participant.setSchedule(this);
    }

    public List<LocationJpaEntity> getLocations() {
        return locations;
    }

    public void addLocation(LocationJpaEntity location) {
        locations.add(location);
        location.setSchedule(this);
    }

    public List<MenuJpaEntity> getMenus() {
        return menus;
    }

    public void addMenu(MenuJpaEntity menu) {
        menus.add(menu);
        menu.setSchedule(this);
    }
}
