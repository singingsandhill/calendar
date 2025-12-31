package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateMenuException extends BusinessException {

    public DuplicateMenuException(String name) {
        super("DUPLICATE_MENU",
                "Menu '" + name + "' already exists in this schedule",
                HttpStatus.CONFLICT);
    }
}
