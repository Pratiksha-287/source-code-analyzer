
package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileDiscoveryResult {

    private final int totalFiles;

    private final Map<String, Integer> fileCountsByType;

    private final List<String> stylesheetFiles;

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
                immutableFiles.put(fileType, List.copyOf(paths)));

        this.categorizedFiles =
                Collections.unmodifiableMap(immutableFiles);

        // Calculate file counts, excluding file types with zero files.
        this.fileCountsByType = new LinkedHashMap<>();

        categorizedFiles.forEach((fileType, paths) -> {

            int count = paths.size();

            if (count > 0) {
                this.fileCountsByType.put(fileType, count);
            }
        });

        // Calculate total discovered files.
        this.totalFiles = categorizedFiles.values()
                .stream()
                .mapToInt(List::size)
                .sum();

        // Collect CSS and SCSS file paths without analyzing their contents.
        this.stylesheetFiles = new ArrayList<>();

        addRelativePaths("CSS");
        addRelativePaths("STYLESHEETS");

        Collections.sort(this.stylesheetFiles);
    }

    private void addRelativePaths(String fileType) {

        List<Path> paths = categorizedFiles.getOrDefault(
                fileType,
                List.of()
        );

        for (Path file : paths) {

            Path relativePath = repositoryPath
                    .toAbsolutePath()
                    .normalize()
                    .relativize(
                            file.toAbsolutePath().normalize()
                    );

            stylesheetFiles.add(
                    relativePath.toString().replace('\\', '/')
            );
        }
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public Map<String, Integer> getFileCountsByType() {
        return fileCountsByType;
    }

    public List<String> getStylesheetFiles() {
        return stylesheetFiles;
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
