package me.singingsandhill.calendar.runner.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.runner.domain.Attendance;
import me.singingsandhill.calendar.runner.domain.AttendanceRankingDto;
import me.singingsandhill.calendar.runner.domain.AttendanceRepository;
import me.singingsandhill.calendar.runner.domain.DistanceRankingDto;
import me.singingsandhill.calendar.runner.domain.MemberAttendanceStatsDto;
import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.AttendanceJpaEntity;
import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.RunJpaEntity;
import me.singingsandhill.calendar.runner.infrastructure.persistence.repository.AttendanceJpaRepository;
import me.singingsandhill.calendar.runner.infrastructure.persistence.repository.RunJpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AttendanceRepositoryAdapter implements AttendanceRepository {

    private final AttendanceJpaRepository attendanceJpaRepository;
    private final RunJpaRepository runJpaRepository;

    public AttendanceRepositoryAdapter(AttendanceJpaRepository attendanceJpaRepository,
                                        RunJpaRepository runJpaRepository) {
        this.attendanceJpaRepository = attendanceJpaRepository;
        this.runJpaRepository = runJpaRepository;
    }

    @Override
    public Optional<Attendance> findById(Long id) {
        return attendanceJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Attendance> findByRunId(Long runId) {
        return attendanceJpaRepository.findByRun_Id(runId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Attendance save(Attendance attendance) {
        RunJpaEntity run = runJpaRepository.findById(attendance.getRunId())
                .orElseThrow(() -> new IllegalStateException("Run not found: " + attendance.getRunId()));

        AttendanceJpaEntity entity = new AttendanceJpaEntity(
                run,
                attendance.getParticipantName(),
                attendance.getDistance(),
                attendance.getCreatedAt()
        );

        AttendanceJpaEntity saved = attendanceJpaRepository.save(entity);
        Attendance result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void deleteById(Long id) {
        attendanceJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByRunIdAndParticipantName(Long runId, String participantName) {
        return attendanceJpaRepository.existsByRunIdAndParticipantNameIgnoreCase(runId, participantName);
    }

    @Override
    public List<AttendanceRankingDto> findTop10ByAttendanceCount() {
        return attendanceJpaRepository.findAllGroupedByParticipantOrderByCount().stream()
                .limit(10)
                .map(row -> new AttendanceRankingDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<DistanceRankingDto> findTop10ByTotalDistance() {
        return attendanceJpaRepository.findAllGroupedByParticipantOrderByDistance().stream()
                .limit(10)
                .map(row -> new DistanceRankingDto(
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberAttendanceStatsDto> findAllMemberStats() {
        return attendanceJpaRepository.findAllMemberAttendanceStats().stream()
                .map(row -> new MemberAttendanceStatsDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Attendance> findByParticipantName(String participantName) {
        return attendanceJpaRepository.findByParticipantNameWithRun(participantName).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Attendance toDomain(AttendanceJpaEntity entity) {
        return new Attendance(
                entity.getId(),
                entity.getRunId(),
                entity.getParticipantName(),
                entity.getDistance(),
                entity.getCreatedAt()
        );
    }
}
