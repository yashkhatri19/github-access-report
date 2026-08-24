package com.example.githubaccessreport.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds the single WebClient used to talk to the GitHub REST API.
 *
 * Authentication: a GitHub Personal Access Token (classic or fine-grained) is
 * sent as a Bearer token if configured. If no token is provided, requests
 * proceed unauthenticated (subject to GitHub rate limits).
 */
@Configuration
public class GithubWebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GithubWebClientConfig.class);

    @Bean
    public WebClient githubWebClient(GithubProperties properties,
                                      @Value("${spring.application.name:github-access-report}") String appName) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, appName);

        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken());
            log.info("GitHub WebClient configured with Bearer token authentication.");
        } else {
            log.warn("GITHUB_TOKEN is not configured. WebClient will send unauthenticated requests (subject to 60 requests/hour rate limit).");
        }

        return builder.build();
    }
}
