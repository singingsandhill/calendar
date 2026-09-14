package me.singingsandhill.calendar.runner.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository {

    Optional<Attendance> findById(Long id);

    List<Attendance> findByRunId(Long runId);

    Attendance save(Attendance attendance);

    void deleteById(Long id);

    boolean existsByRunIdAndParticipantName(Long runId, String participantName);

    List<AttendanceRankingDto> findTop10ByAttendanceCount();

    List<DistanceRankingDto> findTop10ByTotalDistance();

    List<MemberAttendanceStatsDto> findAllMemberStats();

    /**
     * 런 날짜 범위(경계 포함) 내 출석만 집계. 경계는 non-null — null 무제한 처리는 서비스 레이어 책임.
     */
    List<MemberAttendanceStatsDto> findMemberStatsByRunDateBetween(LocalDate from, LocalDate to);

    List<Attendance> findByParticipantName(String participantName);
}
