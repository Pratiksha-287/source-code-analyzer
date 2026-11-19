
package org.example.sourcecodeanalyser.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
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

        Map<String, List<Path>> immutableFiles = new LinkedHashMap<>();

        categorizedFiles.forEach((fileType, paths) ->
                immutableFiles.put(
                        fileType,
                        List.copyOf(paths)
                )
        );

        this.categorizedFiles =
                Collections.unmodifiableMap(immutableFiles);

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
