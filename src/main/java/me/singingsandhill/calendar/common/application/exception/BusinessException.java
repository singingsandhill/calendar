package me.singingsandhill.calendar.common.application.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String messageKey;
    private final Object[] messageArgs;

    protected BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, null, null);
    }

    protected BusinessException(String code, String fallbackMessage, HttpStatus status,
                                String messageKey, Object[] messageArgs) {
        super(fallbackMessage);
        this.code = code;
        this.status = status;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
