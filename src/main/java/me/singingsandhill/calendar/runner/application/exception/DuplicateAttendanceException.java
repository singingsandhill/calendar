package me.singingsandhill.calendar.runner.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateAttendanceException extends BusinessException {

    public DuplicateAttendanceException(String participantName, Long runId) {
        super("DUPLICATE_ATTENDANCE",
                "Participant '" + participantName + "' already registered for run " + runId,
                HttpStatus.CONFLICT);
    }
}
