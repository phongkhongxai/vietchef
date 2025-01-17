package com.spring2025.vietchefs.models.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus
public class VchefApiException extends RuntimeException{
    private HttpStatus status;
    private String message;

    public VchefApiException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public VchefApiException(String message, HttpStatus status, String message1) {
        super(message);
        this.status = status;
        this.message = message1;
    }
}
