package com.ejobs.portal.repository;

import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.model.Job;
import com.ejobs.portal.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @EntityGraph(attributePaths = "employer")
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "employer")
    List<Job> findByEmployerId(UUID employerId);

    /** Platform stats. */
    long countByStatus(JobStatus status);

    /**
     * FR-JOB-04: browse/search with every filter optional.
     * <p>
     * Two deliberate details here:
     * <ul>
     *   <li>Predicates are guarded with {@code :param IS NULL OR ...} rather than
     *       {@code COALESCE(:param, j.column)}. COALESCE is unsafe because
     *       {@code location} is nullable: {@code j.location = COALESCE(NULL, j.location)}
     *       becomes {@code NULL = NULL} (UNKNOWN), so jobs with no location would
     *       silently vanish from an unfiltered search.</li>
     *   <li>The String params are wrapped in {@code CAST(... AS String)}. PostgreSQL
     *       cannot infer the type of a null bind parameter inside CONCAT and falls back
     *       to {@code bytea}, which fails at runtime with
     *       {@code function lower(bytea) does not exist}.</li>
     *   <li>The entity graph fetches the employer alongside the page. JobSummaryResponse
     *       exposes employerName, and without this each distinct employer on the page
     *       costs its own select. Safe with Pageable because employer is single-valued -
     *       fetching a collection here would force in-memory pagination.</li>
     * </ul>
     */
    @EntityGraph(attributePaths = "employer")
    @Query(value = """
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:employmentType IS NULL OR j.employmentType = :employmentType)
              AND (CAST(:location AS String) IS NULL
                   OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS String), '%')))
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(j.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """,
            countQuery = """
            SELECT COUNT(j) FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:employmentType IS NULL OR j.employmentType = :employmentType)
              AND (CAST(:location AS String) IS NULL
                   OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS String), '%')))
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(j.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
            """)
    Page<Job> search(@Param("status") JobStatus status,
                     @Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("employmentType") EmploymentType employmentType,
                     Pageable pageable);
}
