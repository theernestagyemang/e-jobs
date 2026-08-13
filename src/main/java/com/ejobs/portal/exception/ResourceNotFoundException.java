package com.ejobs.portal.exception;

/** Raised when a requested user, job, or application does not exist. Maps to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
