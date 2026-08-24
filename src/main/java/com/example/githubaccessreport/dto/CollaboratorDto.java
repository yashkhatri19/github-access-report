package com.example.githubaccessreport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the subset of fields we care about from
 * GET /repos/{owner}/{repo}/collaborators
 * https://docs.github.com/en/rest/collaborators/collaborators#list-repository-collaborators
 *
 * "role_name" is the simplest single string describing the effective
 * permission level (e.g. "admin", "maintain", "write", "triage", "read",
 * or a custom repository role name), so we surface that directly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollaboratorDto {

    private String login;

    @JsonProperty("role_name")
    private String roleName;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
