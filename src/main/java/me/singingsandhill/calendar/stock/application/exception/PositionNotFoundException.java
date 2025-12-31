package me.singingsandhill.calendar.stock.application.exception;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PositionNotFoundException extends BusinessException {
    public PositionNotFoundException(Long id) {
        super("POSITION_NOT_FOUND", "Position not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public PositionNotFoundException(String stockCode) {
        super("POSITION_NOT_FOUND", "No open position for stock: " + stockCode, HttpStatus.NOT_FOUND);
    }
}
