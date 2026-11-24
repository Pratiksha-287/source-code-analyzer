package org.example.sourcecodeanalyser.discovery;

import org.example.sourcecodeanalyser.model.module.ModuleInfo;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ModuleDiscovery {

    public List<ModuleInfo> discoverModules(Path modulesDirectory) {

        if (modulesDirectory == null) {
            return List.of();
        }

        if (!Files.isDirectory(modulesDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid modules directory: " + modulesDirectory
            );
        }

        List<ModuleInfo> discoveredModules = new ArrayList<>();

        try (var directories = Files.list(modulesDirectory)) {

            directories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .forEach(directory -> {

                        Path buildFile = findBuildFile(directory);
                        Path bndFile = directory.resolve("bnd.bnd");

                        // A directory is considered a module
                        // if it contains a Gradle build file.
                        if (buildFile != null) {

                            ModuleInfo moduleInfo = new ModuleInfo(
                                    directory.getFileName().toString(),
                                    directory,
                                    buildFile,
                                    Files.isRegularFile(bndFile) ? bndFile : null
                            );

                            discoveredModules.add(moduleInfo);
                        }
                    });

        } catch (java.io.IOException exception) {
            throw new RuntimeException(
                    "Failed to discover modules in: " + modulesDirectory,
                    exception
            );
        }

        return List.copyOf(discoveredModules);
    }

    private Path findBuildFile(Path directory) {

        Path groovyBuildFile = directory.resolve("build.gradle");

        Path kotlinBuildFile = directory.resolve("build.gradle.kts");

        if (Files.isRegularFile(groovyBuildFile)) {
            return groovyBuildFile;
        }

        if (Files.isRegularFile(kotlinBuildFile)) {
            return kotlinBuildFile;
        }

        return null;
    }
}