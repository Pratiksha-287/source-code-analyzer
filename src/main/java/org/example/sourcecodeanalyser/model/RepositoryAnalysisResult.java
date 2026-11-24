
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class RepositoryAnalysisResult {

    private String schemaVersion;
    private FileDiscoveryResult fileDiscovery;
    private JavaAnalysisResult javaAnalysis;
    private List<CSSAnalysisResult> cssAnalysis;
    private List<SCSSAnalysisResult> scssAnalysis;

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<CSSAnalysisResult> cssAnalysis,
            List<SCSSAnalysisResult> scssAnalysis) {

        this.schemaVersion = "1.0";
        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;
        this.cssAnalysis = cssAnalysis;
        this.scssAnalysis = scssAnalysis;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public FileDiscoveryResult getFileDiscovery() {
        return fileDiscovery;
    }

    public JavaAnalysisResult getJavaAnalysis() {
        return javaAnalysis;
    }

    public List<CSSAnalysisResult> getCssAnalysis() {
        return cssAnalysis;
    }

    public List<SCSSAnalysisResult> getScssAnalysis() {
        return scssAnalysis;
    }
}
