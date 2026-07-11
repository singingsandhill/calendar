package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class OwnerAlreadyLinkedException extends BusinessException {

    public OwnerAlreadyLinkedException(String ownerId) {
        super("OWNER_ALREADY_LINKED",
                "Owner is already linked to another user: " + ownerId,
                HttpStatus.CONFLICT);
    }
}
