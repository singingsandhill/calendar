package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateParticipantException extends BusinessException {

    public DuplicateParticipantException(String name) {
        super("DUPLICATE_PARTICIPANT",
                "Participant with name '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
