package org.example.sourcecodeanalyser.model.module;

import java.nio.file.Path;
import java.util.List;

public class GradleInfo {

    private String moduleName;
    private Path buildFilePath;
    private String group;
    private List<String> plugins;
    private List<GradleDependencyInfo> dependencies;
    private List<String> repositories;

    public GradleInfo(
            String moduleName,
            Path buildFilePath,
            String group,
            List<String> plugins,
            List<GradleDependencyInfo> dependencies,
            List<String> repositories
    ) {
        this.moduleName = moduleName;
        this.buildFilePath = buildFilePath;
        this.group = group;
        this.plugins = plugins;
        this.dependencies = dependencies;
        this.repositories = repositories;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getBuildFilePath() {
        return buildFilePath;
    }

    public String getGroup() {
        return group;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public List<GradleDependencyInfo> getDependencies() {
        return dependencies;
    }

    public List<String> getRepositories() {
        return repositories;
    }
}