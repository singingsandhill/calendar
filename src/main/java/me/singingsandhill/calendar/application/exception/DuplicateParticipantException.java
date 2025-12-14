package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class DuplicateParticipantException extends BusinessException {

    public DuplicateParticipantException(String name) {
        super("DUPLICATE_PARTICIPANT",
                "Participant with name '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
