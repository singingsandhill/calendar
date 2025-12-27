package me.singingsandhill.calendar.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_attendance_run_participant",
           columnNames = {"run_id", "participant_name"}
       ))
public class AttendanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private RunJpaEntity run;

    @Column(name = "participant_name", nullable = false, length = 50)
    private String participantName;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal distance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AttendanceJpaEntity() {}

    public AttendanceJpaEntity(RunJpaEntity run, String participantName,
                                BigDecimal distance, LocalDateTime createdAt) {
        this.run = run;
        this.participantName = participantName;
        this.distance = distance;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RunJpaEntity getRun() {
        return run;
    }

    public Long getRunId() {
        return run != null ? run.getId() : null;
    }

    public String getParticipantName() {
        return participantName;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
