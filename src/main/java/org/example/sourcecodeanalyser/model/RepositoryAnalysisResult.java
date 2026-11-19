
package org.example.sourcecodeanalyser.model;

public class RepositoryAnalysisResult {

    private FileDiscoveryResult fileDiscovery;
    private JavaAnalysisResult javaAnalysis;

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis) {

        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;
    }

    public FileDiscoveryResult getFileDiscovery() {
        return fileDiscovery;
    }

    public JavaAnalysisResult getJavaAnalysis() {
        return javaAnalysis;
    }
}
