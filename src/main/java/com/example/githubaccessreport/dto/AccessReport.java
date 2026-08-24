package com.example.githubaccessreport.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Top level response returned by GET /api/access-report.
 *
 * userAccess maps a GitHub login -> the list of repositories (with the
 * permission level) that user can access within the organization.
 */
public class AccessReport {

    private final String organization;
    private final Instant generatedAt;
    private final int totalRepositories;
    private final int totalUsers;
    private final Map<String, List<RepositoryAccess>> userAccess;

    public AccessReport(String organization,
                         Instant generatedAt,
                         int totalRepositories,
                         int totalUsers,
                         Map<String, List<RepositoryAccess>> userAccess) {
        this.organization = organization;
        this.generatedAt = generatedAt;
        this.totalRepositories = totalRepositories;
        this.totalUsers = totalUsers;
        this.userAccess = userAccess;
    }

    public String getOrganization() {
        return organization;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public int getTotalRepositories() {
        return totalRepositories;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public Map<String, List<RepositoryAccess>> getUserAccess() {
        return userAccess;
    }
}
