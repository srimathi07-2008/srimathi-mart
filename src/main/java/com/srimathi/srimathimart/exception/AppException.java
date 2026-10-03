package com.srimathi.srimathimart.exception;

/** Base type for all application level failures. */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AppException(final String message) {
        super(message);
    }

    public AppException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
