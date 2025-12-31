package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class OwnerNotFoundException extends BusinessException {

    public OwnerNotFoundException(String ownerId) {
        super("OWNER_NOT_FOUND", "Owner not found: " + ownerId, HttpStatus.NOT_FOUND);
    }
}
