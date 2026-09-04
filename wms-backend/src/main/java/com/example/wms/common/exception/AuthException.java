package com.example.wms.common.exception;

import java.io.Serial;

public class AuthException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }
}
