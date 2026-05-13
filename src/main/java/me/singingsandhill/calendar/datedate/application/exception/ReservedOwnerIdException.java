package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class ReservedOwnerIdException extends BusinessException {

    public static final String MESSAGE_KEY = "errors.idReservedOrTaken";

    public ReservedOwnerIdException(String ownerId) {
        super("RESERVED_OWNER_ID",
                "This ID is taken or reserved: " + ownerId,
                HttpStatus.BAD_REQUEST,
                MESSAGE_KEY,
                new Object[]{ownerId});
    }
}
