package com.example.githubaccessreport.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Raised whenever a call to the GitHub REST API fails in a way that should
 * be surfaced to the caller of our own API (e.g. bad credentials, org not
 * found, rate limit exhausted after retries, etc).
 */
public class GithubApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public GithubApiException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GithubApiException(String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
