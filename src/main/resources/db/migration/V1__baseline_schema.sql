-- Baseline: the schema Hibernate's ddl-auto had been generating, captured verbatim so
-- `ddl-auto: validate` passes against it. From here on the DDL is owned by these
-- migrations, not by the entity annotations.

CREATE TABLE users (
    id            uuid                        NOT NULL,
    full_name     character varying(255)      NOT NULL,
    email         character varying(255)      NOT NULL,
    password_hash character varying(255)      NOT NULL,
    role          character varying(255)      NOT NULL,
    active        boolean DEFAULT true        NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    updated_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('JOB_SEEKER', 'EMPLOYER', 'ADMIN'))
);

CREATE TABLE jobs (
    id              uuid                        NOT NULL,
    employer_id     uuid                        NOT NULL,
    title           character varying(255)      NOT NULL,
    department      character varying(255),
    location        character varying(255),
    employment_type character varying(255)      NOT NULL,
    salary_range    character varying(255),
    description     text,
    deadline        date,
    status          character varying(255)      NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT jobs_pkey PRIMARY KEY (id),
    CONSTRAINT fk_jobs_employer FOREIGN KEY (employer_id) REFERENCES users (id),
    CONSTRAINT jobs_employment_type_check CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'REMOTE')),
    CONSTRAINT jobs_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_jobs_employer_id ON jobs USING btree (employer_id);

CREATE TABLE applications (
    id           uuid                        NOT NULL,
    job_id       uuid                        NOT NULL,
    applicant_id uuid                        NOT NULL,
    cover_note   text,
    resume_url   character varying(255),
    status       character varying(255)      NOT NULL,
    applied_at   timestamp(6) with time zone NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT applications_pkey PRIMARY KEY (id),
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_applications_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT uk_applications_job_applicant UNIQUE (job_id, applicant_id),
    CONSTRAINT applications_status_check CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED', 'REJECTED'))
);

CREATE INDEX idx_applications_job_id ON applications USING btree (job_id);
CREATE INDEX idx_applications_applicant_id ON applications USING btree (applicant_id);

CREATE TABLE audit_logs (
    id          uuid                        NOT NULL,
    event_type  character varying(255)      NOT NULL,
    actor_email character varying(255),
    detail      character varying(255),
    created_at  timestamp(6) with time zone NOT NULL,
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
    CONSTRAINT audit_logs_event_type_check CHECK (event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILURE', 'REGISTER', 'USER_DEACTIVATED', 'USER_REACTIVATED'))
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs USING btree (created_at);
