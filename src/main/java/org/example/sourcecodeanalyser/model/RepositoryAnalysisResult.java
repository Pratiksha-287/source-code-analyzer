
package org.example.sourcecodeanalyser.model;

import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;
import org.example.sourcecodeanalyser.model.python.PythonFileAnalysis;

import java.util.List;

public class RepositoryAnalysisResult {

    private final FileDiscoveryResult fileDiscovery;
    private final JavaAnalysisResult javaAnalysis;
    private final List<ClientExtensionAnalysis> clientExtensionAnalysis;
    private final GradleAnalysisResult gradleAnalysis;
    private final List<PythonFileAnalysis> pythonAnalysis;
    private final List<ConfigurationFileAnalysis> configurationAnalysis;

    private final List<PythonEnvironmentVariableReference>
            environmentVariableReferences;

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<ClientExtensionAnalysis> clientExtensionAnalysis,
            GradleAnalysisResult gradleAnalysis,
            List<PythonFileAnalysis> pythonAnalysis) {

        this(
                fileDiscovery,
                javaAnalysis,
                clientExtensionAnalysis,
                gradleAnalysis,
                pythonAnalysis,
                List.of()
        );
    }

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<ClientExtensionAnalysis> clientExtensionAnalysis,
            GradleAnalysisResult gradleAnalysis,
            List<PythonFileAnalysis> pythonAnalysis,
            List<ConfigurationFileAnalysis> configurationAnalysis) {

        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;

        this.clientExtensionAnalysis =
                List.copyOf(clientExtensionAnalysis);

        this.gradleAnalysis = gradleAnalysis;

        this.pythonAnalysis =
                List.copyOf(pythonAnalysis);

        this.configurationAnalysis =
                List.copyOf(configurationAnalysis);

        // Aggregate Python environment-variable references
        // while retaining the source Python file.
        this.environmentVariableReferences =
                this.pythonAnalysis.stream()
                        .filter(pythonFile -> pythonFile.environmentVariables() != null)
                        .flatMap(pythonFile ->
                                pythonFile.environmentVariables()
                                        .stream()
                                        .map(reference ->
                                                new PythonEnvironmentVariableReference(
                                                        pythonFile.filePath(),
                                                        reference
                                                )
                                        )
                        )
                        .toList();
    }

    public FileDiscoveryResult getFileDiscovery() {
        return fileDiscovery;
    }

    public JavaAnalysisResult getJavaAnalysis() {
        return javaAnalysis;
    }

    public List<ClientExtensionAnalysis> getClientExtensionAnalysis() {
        return clientExtensionAnalysis;
    }

    public GradleAnalysisResult getGradleAnalysis() {
        return gradleAnalysis;
    }

    public List<PythonFileAnalysis> getPythonAnalysis() {
        return pythonAnalysis;
    }

    public List<ConfigurationFileAnalysis> getConfigurationAnalysis() {
        return configurationAnalysis;
    }

    public List<PythonEnvironmentVariableReference>
    getEnvironmentVariableReferences() {
        return environmentVariableReferences;
    }

    /**
     * Represents an environment-variable reference found
     * inside a Python source file.
     */
    public record PythonEnvironmentVariableReference(
            String filePath,
            PythonFileAnalysis.PythonEnvironmentVariableInfo reference
    ) {
    }
}
