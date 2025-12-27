package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class RunNotFoundException extends BusinessException {

    public RunNotFoundException(Long runId) {
        super("RUN_NOT_FOUND",
                "Run not found with id: " + runId,
                HttpStatus.NOT_FOUND);
    }
}
