package com.example.githubaccessreport.controller;

import com.example.githubaccessreport.dto.AccessReport;
import com.example.githubaccessreport.service.AccessReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessReportController {

    private final AccessReportService accessReportService;

    public AccessReportController(AccessReportService accessReportService) {
        this.accessReportService = accessReportService;
    }

    /**
     * GET /api/access-report?org=my-org
     *
     * Returns a JSON report mapping every user who has access to at least
     * one repository in the organization to the list of repositories (and
     * permission level) they can access.
     *
     * Example:
     * GET http://localhost:8080/api/access-report?org=octocat-inc
     */
    @GetMapping(value = "/api/access-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccessReport getAccessReport(@RequestParam("org") String org) {
        return accessReportService.generateReport(org);
    }
}
