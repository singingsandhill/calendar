package me.singingsandhill.calendar.stock.application.exception;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super("INSUFFICIENT_BALANCE",
              String.format("Insufficient balance. Required: %s, Available: %s", required, available),
              HttpStatus.BAD_REQUEST);
    }
}
