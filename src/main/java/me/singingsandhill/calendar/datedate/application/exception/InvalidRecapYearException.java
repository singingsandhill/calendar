package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class InvalidRecapYearException extends BusinessException {

    public InvalidRecapYearException(int year) {
        super("INVALID_RECAP_YEAR",
                "Recap year out of range: " + year,
                HttpStatus.BAD_REQUEST);
    }
}
