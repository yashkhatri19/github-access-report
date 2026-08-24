package com.example.githubaccessreport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the subset of fields we care about from
 * GET /orgs/{org}/repos
 * https://docs.github.com/en/rest/repos/repos#list-organization-repositories
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryDto {

    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private boolean archived;
    private boolean disabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
