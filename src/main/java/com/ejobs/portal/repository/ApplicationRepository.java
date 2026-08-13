package com.ejobs.portal.repository;

import com.ejobs.portal.model.Application;
import com.ejobs.portal.model.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * FR-APP-02: guard against a duplicate *active* application to the same job.
     *
     * <p>Mirrors the partial unique index uk_applications_job_applicant_active, which
     * ignores REJECTED rows - a turned-down candidate is free to apply again.
     */
    boolean existsByJobIdAndApplicantIdAndStatusNot(UUID jobId,
                                                    UUID applicantId,
                                                    ApplicationStatus status);

    /**
     * FR-APP-03: employer pipeline view - every application across that employer's jobs.
     *
     * <p>ApplicationResponse reads job.title and applicant.fullName, both LAZY. Without
     * the graph each row costs extra selects; with it the whole list is one query.
     */
    @EntityGraph(attributePaths = {"job", "applicant"})
    List<Application> findByJobEmployerId(UUID employerId);

    /** FR-APP-05: job seeker dashboard. Same mapping, same reason for the graph. */
    @EntityGraph(attributePaths = {"job", "applicant"})
    List<Application> findByApplicantId(UUID applicantId);
}
