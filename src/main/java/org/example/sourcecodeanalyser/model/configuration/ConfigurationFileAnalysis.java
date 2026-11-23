
package org.example.sourcecodeanalyser.model.configuration;

import java.util.List;

public class ConfigurationFileAnalysis {

    private final String filePath;
    private final String fileType;
    private final List<String> keys;
    private final List<ConfigurationKeyAnalysis> keyDetails;
    private final List<ConfigurationReference> references;
    private final String environment;
    private final String status;
    private final List<String> diagnostics;

    public ConfigurationFileAnalysis(
            String filePath,
            String fileType,
            List<String> keys) {
        this(filePath, fileType, keys, List.of());
    }

    public ConfigurationFileAnalysis(
            String filePath,
            String fileType,
            List<String> keys,
            List<String> diagnostics) {
        this(
                filePath,
                fileType,
                keys,
                List.of(),
                List.of(),
                "COMMON",
                diagnostics.isEmpty() ? "PARSED" : "PARSED_WITH_WARNINGS",
                diagnostics
        );
    }

    public ConfigurationFileAnalysis(
            String filePath,
            String fileType,
            List<String> keys,
            List<ConfigurationKeyAnalysis> keyDetails,
            List<ConfigurationReference> references,
            String environment,
            String status,
            List<String> diagnostics) {

        this.filePath = filePath;
        this.fileType = fileType;
        this.keys = List.copyOf(keys);
        this.keyDetails = List.copyOf(keyDetails);
        this.references = List.copyOf(references);
        this.environment = environment;
        this.status = status;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<ConfigurationKeyAnalysis> getKeyDetails() {
        return keyDetails;
    }

    public List<ConfigurationReference> getReferences() {
        return references;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }
}
