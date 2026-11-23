
package org.example.sourcecodeanalyser.model.configuration;

public class ConfigurationReference {

    private final String sourceKey;
    private final String target;
    private final boolean resolved;

    public ConfigurationReference(
            String sourceKey,
            String target,
            boolean resolved) {
        this.sourceKey = sourceKey;
        this.target = target;
        this.resolved = resolved;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getTarget() {
        return target;
    }

    public boolean isResolved() {
        return resolved;
    }
}
