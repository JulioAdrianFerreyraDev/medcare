package com.medcare.shared.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    public DomainException(String message) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, "Domain Rule Violation", message);
    }

    public DomainException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
