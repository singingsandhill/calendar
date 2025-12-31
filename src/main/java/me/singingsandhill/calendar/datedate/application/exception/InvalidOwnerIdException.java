package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class InvalidOwnerIdException extends BusinessException {

    public InvalidOwnerIdException(String message) {
        super("INVALID_OWNER_ID", message, HttpStatus.BAD_REQUEST);
    }
}
