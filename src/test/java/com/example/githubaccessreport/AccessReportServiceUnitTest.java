package com.example.githubaccessreport;

import com.example.githubaccessreport.dto.AccessReport;
import com.example.githubaccessreport.dto.RepositoryAccess;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lightweight unit test for the AccessReport DTO's aggregation shape.
 * (Full integration tests against the real GitHub API would require a
 * network call and a valid token, so they are intentionally out of scope
 * for this assignment-sized project — see README for notes on how this
 * would be extended with WireMock for true service-layer tests.)
 */
class AccessReportServiceUnitTest {

    @Test
    void accessReportExposesAggregatedUserData() {
        Map<String, List<RepositoryAccess>> userAccess = Map.of(
                "octocat", List.of(new RepositoryAccess("my-org/repo-a", "admin")),
                "hubot", List.of(
                        new RepositoryAccess("my-org/repo-a", "read"),
                        new RepositoryAccess("my-org/repo-b", "write"))
        );

        AccessReport report = new AccessReport("my-org", Instant.now(), 2, 2, userAccess);

        assertEquals(2, report.getTotalRepositories());
        assertEquals(2, report.getTotalUsers());
        assertEquals(2, report.getUserAccess().get("hubot").size());
    }
}
