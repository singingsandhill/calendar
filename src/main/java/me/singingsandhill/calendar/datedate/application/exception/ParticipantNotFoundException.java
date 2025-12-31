package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class ParticipantNotFoundException extends BusinessException {

    public ParticipantNotFoundException(Long participantId) {
        super("PARTICIPANT_NOT_FOUND", "Participant not found: " + participantId, HttpStatus.NOT_FOUND);
    }
}
