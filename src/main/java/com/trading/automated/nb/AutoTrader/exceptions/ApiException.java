package com.trading.automated.nb.AutoTrader.exceptions;

public class ApiException extends Exception {

    private final String errorCode;

    public ApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "ApiException [message=" + getMessage() + ", errorCode=" + errorCode + "]";
    }
}