package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class DuplicateScheduleException extends BusinessException {

    public DuplicateScheduleException(String ownerId, int year, int month) {
        super("DUPLICATE_SCHEDULE",
                String.format("Schedule already exists for owner '%s' in %d/%d", ownerId, year, month),
                HttpStatus.CONFLICT);
    }
}
