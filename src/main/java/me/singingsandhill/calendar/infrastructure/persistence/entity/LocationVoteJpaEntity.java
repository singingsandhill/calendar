package me.singingsandhill.calendar.infrastructure.persistence.entity;

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
@Table(name = "location_votes")
public class LocationVoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationJpaEntity location;

    @Column(length = 10, nullable = false)
    private String voterName;

    protected LocationVoteJpaEntity() {
    }

    public LocationVoteJpaEntity(LocationJpaEntity location, String voterName) {
        this.location = location;
        this.voterName = voterName;
    }

    public Long getId() {
        return id;
    }

    public LocationJpaEntity getLocation() {
        return location;
    }

    public void setLocation(LocationJpaEntity location) {
        this.location = location;
    }

    public String getVoterName() {
        return voterName;
    }
}
