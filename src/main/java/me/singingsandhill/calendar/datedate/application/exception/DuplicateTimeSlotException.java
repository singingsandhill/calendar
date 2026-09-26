package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class DuplicateTimeSlotException extends BusinessException {

    public DuplicateTimeSlotException(int dayIndex, int startMinute, int endMinute) {
        super("DUPLICATE_TIME_SLOT",
                String.format("Time slot already exists: day %d, %d-%d", dayIndex, startMinute, endMinute),
                HttpStatus.CONFLICT);
    }
}
