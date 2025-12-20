package me.singingsandhill.calendar.application.exception;

import org.springframework.http.HttpStatus;

public class MenuNotFoundException extends BusinessException {

    public MenuNotFoundException(Long menuId) {
        super("MENU_NOT_FOUND",
                "Menu not found with id: " + menuId,
                HttpStatus.NOT_FOUND);
    }
}
