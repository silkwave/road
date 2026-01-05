package com.example.road.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateServerException extends RuntimeException {
    public DuplicateServerException(String message) {
        super(message);
    }

    public DuplicateServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
