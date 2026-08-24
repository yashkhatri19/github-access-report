package com.example.githubaccessreport.service;

import com.example.githubaccessreport.dto.AccessReport;

/**
 * Service interface for generating GitHub organization access reports.
 */
public interface AccessReportService {

    /**
     * Generates an access report mapping users to repository permissions for an organization.
     *
     * @param organization the GitHub organization name
     * @return AccessReport containing user access mappings
     */
    AccessReport generateReport(String organization);
}
