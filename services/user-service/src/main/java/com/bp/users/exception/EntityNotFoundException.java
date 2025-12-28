package com.bp.users.exception;

/**
 * The type Entity not found exception.
 */
public class EntityNotFoundException extends RuntimeException {
    /**
     * Instantiates a new Entity not found exception.
     *
     * @param message the message
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Entity not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
