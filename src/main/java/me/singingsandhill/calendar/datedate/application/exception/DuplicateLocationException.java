package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateLocationException extends BusinessException {

    public DuplicateLocationException(String name) {
        super("DUPLICATE_LOCATION",
                "Location '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
