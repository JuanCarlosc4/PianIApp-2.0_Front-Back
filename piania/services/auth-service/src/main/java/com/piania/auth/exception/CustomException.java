package com.piania.auth.exception;

public class CustomException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public CustomException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
