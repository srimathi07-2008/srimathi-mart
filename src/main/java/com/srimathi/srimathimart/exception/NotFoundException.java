package com.srimathi.srimathimart.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class NotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(final String message) {
        super(message);
    }
}
