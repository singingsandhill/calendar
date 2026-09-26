package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class TimeSlotNotFoundException extends BusinessException {

    public TimeSlotNotFoundException(Long timeSlotId) {
        super("TIME_SLOT_NOT_FOUND",
                "Time slot not found with id: " + timeSlotId,
                HttpStatus.NOT_FOUND);
    }
}
