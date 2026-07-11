package me.singingsandhill.calendar.common.presentation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

@ControllerAdvice(annotations = Controller.class)
public class MvcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e) {
        log.warn("Business exception in MVC controller: {}", e.getMessage());
        ModelAndView mav = new ModelAndView();
        int status = e.getStatus().value();
        if (status >= 400 && status < 500) {
            mav.setViewName("error/4xx");
            mav.setStatus(e.getStatus());
        } else {
            mav.setViewName("error/5xx");
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        log.error("Unhandled exception in MVC controller: {}", e.getMessage(), e);
        return "error/5xx";
    }
}
