package org.example.sourcecodeanalyser.dto;

public class RepositoryScanRequest {

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
