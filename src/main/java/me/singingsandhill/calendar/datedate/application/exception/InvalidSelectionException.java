package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class InvalidSelectionException extends BusinessException {

    public InvalidSelectionException(String message) {
        super("INVALID_SELECTION", message, HttpStatus.BAD_REQUEST);
    }

    public InvalidSelectionException(int day, int maxDay) {
        super("INVALID_SELECTION",
                String.format("Invalid day selection: %d. Must be between 1 and %d", day, maxDay),
                HttpStatus.BAD_REQUEST);
    }
}
