package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class DuplicateLocationException extends BusinessException {

    public DuplicateLocationException(String name) {
        super("DUPLICATE_LOCATION",
                "Location '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
