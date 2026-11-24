package org.example.sourcecodeanalyser.discovery;

import org.example.sourcecodeanalyser.model.common.RepositoryInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//What kind of repository is this, and which important directories does it contain?
@Service
public class RepositoryDiscovery {

    public RepositoryInfo discoverRepository(Path repositoryPath)
            throws IOException {

        // Step 1: Validate the supplied repository path
        if (repositoryPath == null) {
            throw new IllegalArgumentException(
                    "Repository path cannot be null"
            );
        }

        Path rootPath = repositoryPath.toAbsolutePath().normalize();

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException(
                    "Invalid repository directory: " + rootPath
            );
        }

        // Step 2: Get repository name
        String repositoryName = rootPath.getFileName().toString();

        // Step 3: Detect important workspace directories
        Path modulesDirectory = findDirectory(rootPath, "modules");

        Path clientExtensionsDirectory =
                findDirectory(rootPath, "client-extensions");

        Path configsDirectory = findDirectory(rootPath, "configs");

        Path themesDirectory = findDirectory(rootPath, "themes");

        Path warsDirectory = findDirectory(rootPath, "wars");

        // Step 4: Check for Gradle workspace markers
        boolean hasGradleSettings =
                Files.isRegularFile(rootPath.resolve("settings.gradle"))
                        || Files.isRegularFile(rootPath.resolve("settings.gradle.kts"));

        boolean hasGradleProperties =
                Files.isRegularFile(rootPath.resolve("gradle.properties"));

        // Step 5: Determine whether this looks like a Liferay Workspace
        boolean liferayWorkspace =
                hasGradleSettings
                        && (modulesDirectory != null
                        || clientExtensionsDirectory != null
                        || hasGradleProperties);

        // Step 6: Return repository metadata
        return new RepositoryInfo(
                repositoryName,
                rootPath,
                modulesDirectory,
                clientExtensionsDirectory,
                configsDirectory,
                themesDirectory,
                warsDirectory,
                liferayWorkspace
        );
    }

    private Path findDirectory(Path rootPath, String directoryName) {

        Path directory = rootPath.resolve(directoryName);

        if (Files.isDirectory(directory)) {
            return directory;
        }

        return null;
    }
}