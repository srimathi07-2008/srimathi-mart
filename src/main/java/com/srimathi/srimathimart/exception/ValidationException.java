package com.srimathi.srimathimart.exception;

/** Thrown when user supplied input fails a business rule. Maps to HTTP 400. */
public class ValidationException extends AppException {

    private static final long serialVersionUID = 1L;

    public ValidationException(final String message) {
        super(message);
    }
}
