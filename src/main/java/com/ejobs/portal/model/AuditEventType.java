package com.ejobs.portal.model;

/**
 * Values are persisted as strings and constrained by audit_logs_event_type_check.
 * Adding a value here requires a migration that widens that CHECK constraint - see
 * V3__password_reset_and_admin_audit_events.sql.
 */
public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    REGISTER,
    USER_DEACTIVATED,
    USER_REACTIVATED,
    ADMIN_CREATED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED
}
