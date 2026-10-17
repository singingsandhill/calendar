package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class InvalidTimeSlotException extends BusinessException {

    public InvalidTimeSlotException(String message) {
        super("INVALID_TIME_SLOT", message, HttpStatus.BAD_REQUEST);
    }
}
