package me.singingsandhill.calendar.domain.runner;

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

    List<Attendance> findByParticipantName(String participantName);
}
