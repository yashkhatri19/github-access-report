package com.example.githubaccessreport.service;

import com.example.githubaccessreport.config.GithubProperties;
import com.example.githubaccessreport.dto.CollaboratorDto;
import com.example.githubaccessreport.dto.RepositoryDto;
import com.example.githubaccessreport.exception.GithubApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin wrapper around the GitHub REST API.
 *
 * Two things matter for the scale requirements in the assignment (100+
 * repos, 1000+ users):
 *
 *  1. Pagination: GitHub returns at most 100 items per page for both the
 *     "list org repos" and "list repo collaborators" endpoints. We follow
 *     the RFC 5988 "Link" header to walk every page instead of assuming a
 *     fixed number of pages.
 *
 *  2. Concurrency: fetching collaborators is an O(repos) fan-out. Doing it
 *     sequentially (repo by repo) would be slow for 100+ repositories. We
 *     use the reactive WebClient with flatMap(..., concurrency) so up to
 *     `github.concurrency` repositories are queried in parallel, which
 *     respects GitHub's secondary rate limits while still being fast.
 */
@Service
public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient githubWebClient;
    private final GithubProperties properties;

    public GithubService(WebClient githubWebClient, GithubProperties properties) {
        this.githubWebClient = githubWebClient;
        this.properties = properties;
    }

    /**
     * Fetches every (non-archived) repository belonging to the given
     * organization, following pagination.
     */
    public List<RepositoryDto> fetchOrganizationRepositories(String organization) {
        String initialUri = String.format("/orgs/%s/repos?per_page=%d&type=all",
                organization, properties.getPageSize());

        return fetchAllPages(initialUri, RepositoryDto[].class)
                .collectList()
                .block();
    }

    /**
     * Fetches every collaborator (and their permission level) for a single
     * repository, following pagination.
     */
    public Flux<CollaboratorDto> fetchCollaborators(String organization, String repoName) {
        String initialUri = String.format("/repos/%s/%s/collaborators?per_page=%d&affiliation=all",
                organization, repoName, properties.getPageSize());

        return fetchAllPages(initialUri, CollaboratorDto[].class);
    }

    /**
     * Generic pagination helper: fetches a page, emits its items, then
     * follows the "next" Link header (if present) until GitHub reports no
     * further pages.
     */
    private <T> Flux<T> fetchAllPages(String uri, Class<T[]> arrayType) {
        return fetchPage(uri, arrayType)
                .expand(page -> page.nextLink() != null
                        ? fetchPage(page.nextLink(), arrayType)
                        : Mono.empty())
                .flatMapIterable(Page::items);
    }

    private <T> Mono<Page<T>> fetchPage(String uri, Class<T[]> arrayType) {
        return githubWebClient.get()
                .uri(uri)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMap(Mono::error);
                    }
                    String linkHeader = response.headers().asHttpHeaders().getFirst(HttpHeaders.LINK);
                    return response.bodyToMono(arrayType)
                            .map(items -> new Page<>(List.of(items), extractNextLink(linkHeader)));
                })
                .retryWhen(retrySpec())
                .onErrorMap(WebClientResponseException.class, this::toGithubApiException);
    }

    private Retry retrySpec() {
        // GitHub uses 403/429 for both primary and secondary rate limiting.
        // A short, bounded exponential backoff lets transient limits clear
        // without hammering the API or hanging the request forever.
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .filter(this::isRetryable);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return status.value() == 403 || status.value() == 429 || status.is5xxServerError();
        }
        return false;
    }

    private GithubApiException toGithubApiException(WebClientResponseException ex) {
        log.warn("GitHub API returned {} for request: {}", ex.getStatusCode(), ex.getRequest() != null ? ex.getRequest().getURI() : "unknown");
        String message = switch (ex.getStatusCode().value()) {
            case 401 -> "GitHub authentication failed. Check that GITHUB_TOKEN is valid and not expired.";
            case 403 -> "GitHub API access forbidden or rate limit exceeded. Please retry later.";
            case 404 -> "GitHub organization or repository not found, or the token lacks access to it.";
            default -> "GitHub API request failed with status " + ex.getStatusCode();
        };
        return new GithubApiException(message, ex.getStatusCode(), ex);
    }

    private String extractNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * A single fetched page: the items on it, and the URL of the next page
     * (or null if this was the last page).
     */
    private record Page<T>(List<T> items, String nextLink) {
    }
}
