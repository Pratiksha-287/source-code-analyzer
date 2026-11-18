
package org.example.sourcecodeanalyser.scanner;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class RepositoryScanner {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            ".gradle",
            "build",
            "target",
            "node_modules"
    );

    public List<Path> scanRepository(Path repositoryPath)
            throws IOException {

        if (repositoryPath == null) {
            throw new IllegalArgumentException(
                    "Repository path cannot be null"
            );
        }

        Path rootPath = repositoryPath.toAbsolutePath().normalize();

        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException(
                    "Invalid repository directory: " + rootPath
            );
        }

        List<Path> discoveredFiles = new ArrayList<>();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory,
                    BasicFileAttributes attributes) {

                if (!directory.equals(rootPath)) {

                    String directoryName =
                            directory.getFileName().toString();

                    if (EXCLUDED_DIRECTORIES.contains(directoryName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attributes) {

                if (attributes.isRegularFile()) {
                    discoveredFiles.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        discoveredFiles.sort(
                Comparator.comparing(
                        path -> rootPath.relativize(path).toString()
                )
        );

        return discoveredFiles;
    }
}
