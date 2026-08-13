package com.ejobs.portal.exception;

/**
 * Authenticated and holding the right role, but not the owner of the target resource -
 * e.g. an employer editing another employer's job (FR-JOB-02). Maps to 403.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
