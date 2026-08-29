package com.exelynt.booking.exception;

/**
 * Thrown for client-caused, expected validation/business-rule failures.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
