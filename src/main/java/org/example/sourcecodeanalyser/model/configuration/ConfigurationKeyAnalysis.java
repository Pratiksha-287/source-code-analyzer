
package org.example.sourcecodeanalyser.model.configuration;

public class ConfigurationKeyAnalysis {

    private final String name;
    private final String parentKey;
    private final String description;
    private final boolean sensitive;
    private final boolean duplicate;

    public ConfigurationKeyAnalysis(
            String name,
            String parentKey,
            String description,
            boolean sensitive,
            boolean duplicate) {
        this.name = name;
        this.parentKey = parentKey;
        this.description = description;
        this.sensitive = sensitive;
        this.duplicate = duplicate;
    }

    public String getName() {
        return name;
    }

    public String getParentKey() {
        return parentKey;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public boolean isDuplicate() {
        return duplicate;
    }
}
