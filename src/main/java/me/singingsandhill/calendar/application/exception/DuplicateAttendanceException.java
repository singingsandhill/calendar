package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class DuplicateAttendanceException extends BusinessException {

    public DuplicateAttendanceException(String participantName, Long runId) {
        super("DUPLICATE_ATTENDANCE",
                "Participant '" + participantName + "' already registered for run " + runId,
                HttpStatus.CONFLICT);
    }
}
