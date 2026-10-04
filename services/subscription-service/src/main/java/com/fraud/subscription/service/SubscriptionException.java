package com.fraud.subscription.service;

import org.springframework.http.HttpStatus;

public class SubscriptionException extends RuntimeException {

    private final HttpStatus status;

    public SubscriptionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
