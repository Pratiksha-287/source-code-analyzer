
package org.example.sourcecodeanalyser.model;

import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;

import java.util.List;

public class RepositoryAnalysisResult {

    private final FileDiscoveryResult fileDiscovery;
    private final JavaAnalysisResult javaAnalysis;
    private final List<ClientExtensionAnalysis> clientExtensionAnalysis;
    private final GradleAnalysisResult gradleAnalysis;

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<ClientExtensionAnalysis> clientExtensionAnalysis,
            GradleAnalysisResult gradleAnalysis) {

        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;
        this.clientExtensionAnalysis = clientExtensionAnalysis;
        this.gradleAnalysis = gradleAnalysis;
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
}
