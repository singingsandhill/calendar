package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class ParticipantLimitExceededException extends BusinessException {

    private static final int MAX_PARTICIPANTS = 8;

    public ParticipantLimitExceededException() {
        super("PARTICIPANT_LIMIT_EXCEEDED",
                "Maximum number of participants (" + MAX_PARTICIPANTS + ") has been reached",
                HttpStatus.CONFLICT);
    }
}
