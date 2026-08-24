package com.example.githubaccessreport.service;

import com.example.githubaccessreport.config.GithubProperties;
import com.example.githubaccessreport.dto.AccessReport;
import com.example.githubaccessreport.dto.CollaboratorDto;
import com.example.githubaccessreport.dto.RepositoryAccess;
import com.example.githubaccessreport.dto.RepositoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of AccessReportService.
 * Builds the aggregated "which user has access to which repository" report
 * for a GitHub organization.
 */
@Service
public class AccessReportServiceImpl implements AccessReportService {

    private static final Logger log = LoggerFactory.getLogger(AccessReportServiceImpl.class);

    private final GithubService githubService;
    private final GithubProperties properties;

    public AccessReportServiceImpl(GithubService githubService, GithubProperties properties) {
        this.githubService = githubService;
        this.properties = properties;
    }

    @Override
    public AccessReport generateReport(String organization) {
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("organization must not be empty");
        }

        List<RepositoryDto> repositories = githubService.fetchOrganizationRepositories(organization);
        log.info("Fetched {} repositories for organization '{}'", repositories.size(), organization);

        // Thread-safe accumulator: userLogin -> list of RepositoryAccess.
        // Built up concurrently as each repository's collaborators arrive.
        Map<String, List<RepositoryAccess>> userAccess = new ConcurrentHashMap<>();

        Flux.fromIterable(repositories)
                .flatMap(repo -> githubService.fetchCollaborators(organization, repo.getName())
                                .doOnNext(collaborator -> recordAccess(userAccess, repo, collaborator))
                                .onErrorResume(ex -> {
                                    // Don't let one repo's failure abort the whole report.
                                    log.warn("Skipping collaborators for repository '{}': {}",
                                            repo.getFullName(), ex.getMessage());
                                    return Flux.empty();
                                }),
                        properties.getConcurrency())
                .blockLast();

        int totalUsers = userAccess.size();
        log.info("Aggregated access for {} users across {} repositories in '{}'",
                totalUsers, repositories.size(), organization);

        return new AccessReport(organization, Instant.now(), repositories.size(), totalUsers, userAccess);
    }

    private void recordAccess(Map<String, List<RepositoryAccess>> userAccess,
                               RepositoryDto repo,
                               CollaboratorDto collaborator) {
        RepositoryAccess access = new RepositoryAccess(repo.getFullName(), collaborator.getRoleName());
        userAccess
                .computeIfAbsent(collaborator.getLogin(), key -> new CopyOnWriteArrayList<>())
                .add(access);
    }
}
