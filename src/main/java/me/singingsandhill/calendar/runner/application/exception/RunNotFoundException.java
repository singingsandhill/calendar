package me.singingsandhill.calendar.runner.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class RunNotFoundException extends BusinessException {

    public RunNotFoundException(Long runId) {
        super("RUN_NOT_FOUND",
                "Run not found with id: " + runId,
                HttpStatus.NOT_FOUND);
    }
}
