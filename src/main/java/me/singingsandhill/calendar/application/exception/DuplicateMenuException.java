package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class DuplicateMenuException extends BusinessException {

    public DuplicateMenuException(String name) {
        super("DUPLICATE_MENU",
                "Menu '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
