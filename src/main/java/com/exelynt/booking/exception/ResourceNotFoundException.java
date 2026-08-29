package com.exelynt.booking.exception;

/**
 * Thrown when a requested entity (Resource, Reservation, User, ...) cannot be found.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
