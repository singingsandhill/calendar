package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class UseCaseNotFoundException extends BusinessException {

    public UseCaseNotFoundException(String slug) {
        super("USE_CASE_NOT_FOUND", "Use case not found: " + slug, HttpStatus.NOT_FOUND);
    }
}
