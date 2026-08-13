package com.ejobs.portal.exception;

/** FR-APP-02: applicant already applied to this job. Maps to 409. */
public class DuplicateApplicationException extends RuntimeException {

    public DuplicateApplicationException(String message) {
        super(message);
    }
}
