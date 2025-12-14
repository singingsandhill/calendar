package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class ParticipantNotFoundException extends BusinessException {

    public ParticipantNotFoundException(Long participantId) {
        super("PARTICIPANT_NOT_FOUND", "Participant not found: " + participantId, HttpStatus.NOT_FOUND);
    }
}
