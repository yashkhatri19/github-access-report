package com.example.githubaccessreport.dto;

/**
 * A single (repository, permission) pair used inside the aggregated report.
 */
public class RepositoryAccess {

    private final String repository;
    private final String permission;

    public RepositoryAccess(String repository, String permission) {
        this.repository = repository;
        this.permission = permission;
    }

    public String getRepository() {
        return repository;
    }

    public String getPermission() {
        return permission;
    }
}
