package com.sosvideos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TermNotAcceptedException extends RuntimeException {

    public TermNotAcceptedException() {
        super("Você precisa aceitar os termos de uso para acessar este recurso.");
    }
}
