package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateScheduleException extends BusinessException {

    public DuplicateScheduleException(String ownerId, int year, int month) {
        super("DUPLICATE_SCHEDULE",
                String.format("Schedule already exists for owner '%s' in %d/%d", ownerId, year, month),
                HttpStatus.CONFLICT);
    }
}
