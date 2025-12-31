package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

public class MenuNotFoundException extends BusinessException {

    public MenuNotFoundException(Long menuId) {
        super("MENU_NOT_FOUND",
                "Menu not found with id: " + menuId,
                HttpStatus.NOT_FOUND);
    }
}
