package com.ejobs.portal.dto.admin;

public record PlatformStatsResponse(
        long totalUsers,
        long totalEmployers,
        long totalJobSeekers,
        long totalJobs,
        long activeJobs,
        long totalApplications
) {
}
