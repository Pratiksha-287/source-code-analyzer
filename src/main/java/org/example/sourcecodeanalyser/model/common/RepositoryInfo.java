package org.example.sourcecodeanalyser.model.common;

import java.nio.file.Path;

public class RepositoryInfo {

    private final String repositoryName;
    private final Path repositoryPath;

    private final Path modulesDirectory;
    private final Path clientExtensionsDirectory;
    private final Path configsDirectory;
    private final Path themesDirectory;
    private final Path warsDirectory;

    private final boolean liferayWorkspace;

    public RepositoryInfo(
            String repositoryName,
            Path repositoryPath,
            Path modulesDirectory,
            Path clientExtensionsDirectory,
            Path configsDirectory,
            Path themesDirectory,
            Path warsDirectory,
            boolean liferayWorkspace) {

        this.repositoryName = repositoryName;
        this.repositoryPath = repositoryPath;
        this.modulesDirectory = modulesDirectory;
        this.clientExtensionsDirectory = clientExtensionsDirectory;
        this.configsDirectory = configsDirectory;
        this.themesDirectory = themesDirectory;
        this.warsDirectory = warsDirectory;
        this.liferayWorkspace = liferayWorkspace;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }

    public Path getModulesDirectory() {
        return modulesDirectory;
    }

    public Path getClientExtensionsDirectory() {
        return clientExtensionsDirectory;
    }

    public Path getConfigsDirectory() {
        return configsDirectory;
    }

    public Path getThemesDirectory() {
        return themesDirectory;
    }

    public Path getWarsDirectory() {
        return warsDirectory;
    }

    public boolean isLiferayWorkspace() {
        return liferayWorkspace;
    }
}