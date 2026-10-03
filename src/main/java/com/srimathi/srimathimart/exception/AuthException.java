package com.srimathi.srimathimart.exception;

/** Thrown on bad credentials or insufficient privileges. Maps to HTTP 401/403. */
public class AuthException extends AppException {

    private static final long serialVersionUID = 1L;

    public AuthException(final String message) {
        super(message);
    }
}
