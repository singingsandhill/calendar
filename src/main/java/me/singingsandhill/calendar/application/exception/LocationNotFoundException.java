package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class LocationNotFoundException extends BusinessException {

    public LocationNotFoundException(Long locationId) {
        super("LOCATION_NOT_FOUND",
                "Location not found with id: " + locationId,
                HttpStatus.NOT_FOUND);
    }
}
