
package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileDiscoveryResult {

    private final int totalFiles;
    private final Map<String, Integer> fileCountsByType;

    @JsonIgnore
    private final Path repositoryPath;

    @JsonIgnore
    private final Map<String, List<Path>> categorizedFiles;

    public FileDiscoveryResult(
            Path repositoryPath,
            Map<String, List<Path>> categorizedFiles) {

        this.repositoryPath = repositoryPath;

        Map<String, List<Path>> immutableFiles = new LinkedHashMap<>();

        categorizedFiles.forEach((fileType, paths) ->
                immutableFiles.put(fileType, List.copyOf(paths))
        );

        this.categorizedFiles =
                Collections.unmodifiableMap(immutableFiles);

        this.fileCountsByType = new LinkedHashMap<>();

        categorizedFiles.forEach((fileType, paths) ->
                this.fileCountsByType.put(fileType, paths.size())
        );

        this.totalFiles = categorizedFiles.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public Map<String, Integer> getFileCountsByType() {
        return fileCountsByType;
    }

    @JsonIgnore
    public Path getRepositoryPath() {
        return repositoryPath;
    }

    @JsonIgnore
    public Map<String, List<Path>> getCategorizedFiles() {
        return categorizedFiles;
    }
}
