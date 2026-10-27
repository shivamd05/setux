package com.fightclub.setux.management.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    /** Creates an API exception with its HTTP status and message. */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Returns the HTTP status associated with this exception. */
    public HttpStatus getStatus() {
        return status;
    }

}
