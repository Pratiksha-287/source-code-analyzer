package org.example.sourcecodeanalyser.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FileDiscoveryResult {

    private final Path repositoryPath;

    private final int totalFiles;

    private final Map<String, List<Path>> categorizedFiles;

    public FileDiscoveryResult(
            Path repositoryPath,
            Map<String, List<Path>> categorizedFiles) {

        this.repositoryPath = repositoryPath;
        this.categorizedFiles = categorizedFiles;

        this.totalFiles = categorizedFiles.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public Map<String, List<Path>> getCategorizedFiles() {
        return categorizedFiles;
    }
}
