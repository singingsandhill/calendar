package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class ScheduleNotFoundException extends BusinessException {

    public ScheduleNotFoundException(Long scheduleId) {
        super("SCHEDULE_NOT_FOUND", "Schedule not found: " + scheduleId, HttpStatus.NOT_FOUND);
    }

    public ScheduleNotFoundException(String ownerId, int year, int month) {
        super("SCHEDULE_NOT_FOUND",
                String.format("Schedule not found for owner '%s' in %d/%d", ownerId, year, month),
                HttpStatus.NOT_FOUND);
    }
}
