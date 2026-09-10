package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class GuideNotFoundException extends BusinessException {

    public GuideNotFoundException(String slug) {
        super("GUIDE_NOT_FOUND", "Guide not found: " + slug, HttpStatus.NOT_FOUND);
    }
}
