
package org.example.sourcecodeanalyser.frontend.discovery;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FrontendFileDiscoveryService {

    private static final List<String> SUPPORTED_EXTENSIONS =
            List.of(
                    ".js",
                    ".jsx",
                    ".mjs",
                    ".cjs",
                    ".ts",
                    ".tsx",
                    ".mts",
                    ".cts"
            );

    public List<Path> discoverFrontendFiles(Path extensionDirectory) {

        if (extensionDirectory == null ||
                !Files.isDirectory(extensionDirectory)) {
            return List.of();
        }

        try (var paths = Files.walk(extensionDirectory)) {

            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isFrontendFile)
                    .sorted()
                    .toList();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to discover frontend files in: "
                            + extensionDirectory,
                    exception
            );
        }
    }

    private boolean isFrontendFile(Path filePath) {

        String fileName = filePath.getFileName()
                .toString()
                .toLowerCase();

        return SUPPORTED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
    }
}
