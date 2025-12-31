package me.singingsandhill.calendar.runner.application.service;

import me.singingsandhill.calendar.runner.application.exception.DuplicateAttendanceException;
import me.singingsandhill.calendar.runner.application.exception.RunNotFoundException;
import me.singingsandhill.calendar.runner.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RunRepository runRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, RunRepository runRepository) {
        this.attendanceRepository = attendanceRepository;
        this.runRepository = runRepository;
    }

    public List<Attendance> getAttendancesByRunId(Long runId) {
        return attendanceRepository.findByRunId(runId);
    }

    @Transactional
    public Attendance registerAttendance(Long runId, String participantName, BigDecimal distance) {
        if (!runRepository.existsById(runId)) {
            throw new RunNotFoundException(runId);
        }

        if (attendanceRepository.existsByRunIdAndParticipantName(runId, participantName)) {
            throw new DuplicateAttendanceException(participantName, runId);
        }

        Attendance attendance = new Attendance(runId, participantName, distance);
        return attendanceRepository.save(attendance);
    }

    public List<AttendanceRankingDto> getTop10ByAttendanceCount() {
        return attendanceRepository.findTop10ByAttendanceCount();
    }

    public List<DistanceRankingDto> getTop10ByTotalDistance() {
        return attendanceRepository.findTop10ByTotalDistance();
    }

    public List<MemberAttendanceStatsDto> getAllMemberStats() {
        return attendanceRepository.findAllMemberStats();
    }

    public List<Attendance> getAttendancesByParticipantName(String participantName) {
        return attendanceRepository.findByParticipantName(participantName);
    }
}
