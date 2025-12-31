package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class LocationNotFoundException extends BusinessException {

    public LocationNotFoundException(Long locationId) {
        super("LOCATION_NOT_FOUND",
                "Location not found with id: " + locationId,
                HttpStatus.NOT_FOUND);
    }
}
