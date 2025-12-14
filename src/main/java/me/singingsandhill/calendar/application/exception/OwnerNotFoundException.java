package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class OwnerNotFoundException extends BusinessException {

    public OwnerNotFoundException(String ownerId) {
        super("OWNER_NOT_FOUND", "Owner not found: " + ownerId, HttpStatus.NOT_FOUND);
    }
}
