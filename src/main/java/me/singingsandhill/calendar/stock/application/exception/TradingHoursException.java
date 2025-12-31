package me.singingsandhill.calendar.stock.application.exception;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TradingHoursException extends BusinessException {
    public TradingHoursException(String message) {
        super("OUTSIDE_TRADING_HOURS", message, HttpStatus.BAD_REQUEST);
    }
}
