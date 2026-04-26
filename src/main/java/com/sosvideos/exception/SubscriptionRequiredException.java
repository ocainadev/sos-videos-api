package com.sosvideos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SubscriptionRequiredException extends RuntimeException {

    public SubscriptionRequiredException(String message) {
        super(message);
    }
}
