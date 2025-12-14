package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidOwnerIdException extends BusinessException {

    public InvalidOwnerIdException(String message) {
        super("INVALID_OWNER_ID", message, HttpStatus.BAD_REQUEST);
    }
}
