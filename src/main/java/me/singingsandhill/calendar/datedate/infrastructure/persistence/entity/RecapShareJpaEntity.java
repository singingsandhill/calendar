package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recap_shares", uniqueConstraints = {
        @UniqueConstraint(columnNames = "token"),
        @UniqueConstraint(columnNames = {"userId", "shareYear"})
})
public class RecapShareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 'year' 는 SQL 예약어 충돌 위험이 있어 shareYear 로 저장. */
    @Column(name = "shareYear", nullable = false)
    private int year;

    @Column(nullable = false, length = 36)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RecapShareJpaEntity() {
    }

    public RecapShareJpaEntity(Long id, Long userId, int year, String token, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.year = year;
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
