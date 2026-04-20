package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

public class ParticipantLimitExceededException extends BusinessException {

    public ParticipantLimitExceededException() {
        super("PARTICIPANT_LIMIT_EXCEEDED",
                "Maximum number of participants (" + Schedule.MAX_PARTICIPANTS + ") has been reached",
                HttpStatus.CONFLICT);
    }
}
