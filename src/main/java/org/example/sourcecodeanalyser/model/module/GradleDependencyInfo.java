
package org.example.sourcecodeanalyser.model.module;

public class GradleDependencyInfo {

    private String configuration;
    private String dependencyType;
    private String group;
    private String name;
    private String version;
    private String projectPath;
    private String scope;

    public GradleDependencyInfo(
            String configuration,
            String dependencyType,
            String group,
            String name,
            String version,
            String projectPath,
            String scope) {

        this.configuration = configuration;
        this.dependencyType = dependencyType;
        this.group = group;
        this.name = name;
        this.version = version;
        this.projectPath = projectPath;
        this.scope = scope;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getScope() {
        return scope;
    }
}
