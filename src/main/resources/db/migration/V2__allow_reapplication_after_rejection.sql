-- FR-APP-02 says a seeker may not hold a duplicate *active* application to a job.
-- The plain UNIQUE (job_id, applicant_id) from V1 was stricter than that: once an
-- applicant was REJECTED they could never apply to that posting again, and the API
-- answered 409 forever.
--
-- A partial unique index expresses the requirement as written. It cannot be declared
-- with JPA annotations, which is why the entity no longer carries @UniqueConstraint -
-- this file is the single source of truth for the rule.

ALTER TABLE applications
    DROP CONSTRAINT uk_applications_job_applicant;

CREATE UNIQUE INDEX uk_applications_job_applicant_active
    ON applications (job_id, applicant_id)
    WHERE status <> 'REJECTED';
