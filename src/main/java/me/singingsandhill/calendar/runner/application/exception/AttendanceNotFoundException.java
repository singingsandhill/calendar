package me.singingsandhill.calendar.runner.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class AttendanceNotFoundException extends BusinessException {

    public AttendanceNotFoundException(Long attendanceId) {
        super("ATTENDANCE_NOT_FOUND",
                "Attendance not found with id: " + attendanceId,
                HttpStatus.NOT_FOUND);
    }
}
