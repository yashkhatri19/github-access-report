package com.example.githubaccessreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the "github.*" properties from application.yml / environment variables.
 *
 * github.token         -> Personal Access Token (or GitHub App installation token)
 * github.api-base-url  -> Base URL of the GitHub REST API
 * github.page-size     -> Page size used for paginated GitHub endpoints (max 100)
 * github.concurrency   -> Max number of repositories whose collaborators are
 *                         fetched in parallel at any given time.
 */
@ConfigurationProperties(prefix = "github")
public class GithubProperties {

    private String token;
    private String apiBaseUrl = "https://api.github.com";
    private int pageSize = 100;
    private int concurrency = 10;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }
}
