package me.singingsandhill.calendar.stock.application.exception;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StockNotFoundException extends BusinessException {
    public StockNotFoundException(String stockCode) {
        super("STOCK_NOT_FOUND", "Stock not found: " + stockCode, HttpStatus.NOT_FOUND);
    }

    public StockNotFoundException(Long id) {
        super("STOCK_NOT_FOUND", "Stock not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
