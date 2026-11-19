
package org.example.sourcecodeanalyser.dto;

import jakarta.validation.constraints.NotBlank;

public class RepositoryScanRequest {

    @NotBlank(message = "Repository path must not be empty")
    private String repositoryPath;

    public RepositoryScanRequest() {
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
}
