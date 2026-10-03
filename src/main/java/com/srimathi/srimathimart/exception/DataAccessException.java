package com.srimathi.srimathimart.exception;

/** Wraps a checked SQLException so callers need not handle JDBC types. */
public class DataAccessException extends AppException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
