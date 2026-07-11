package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class RecapShareNotFoundException extends BusinessException {

    public RecapShareNotFoundException(String token) {
        super("RECAP_SHARE_NOT_FOUND",
                "Recap share not found: " + token,
                HttpStatus.NOT_FOUND);
    }
}
