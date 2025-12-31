package me.singingsandhill.calendar.stock.application.exception;

import me.singingsandhill.calendar.common.application.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class KisApiException extends BusinessException {
    public KisApiException(String operation, String errorCode, String errorMessage) {
        super("KIS_API_ERROR",
              String.format("KIS API error during %s: [%s] %s", operation, errorCode, errorMessage),
              HttpStatus.BAD_GATEWAY);
    }

    public KisApiException(String operation, String errorMessage) {
        super("KIS_API_ERROR",
              String.format("KIS API error during %s: %s", operation, errorMessage),
              HttpStatus.BAD_GATEWAY);
    }
}
