
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class RepositoryAnalysisResult {

    private FileDiscoveryResult fileDiscovery;
    private JavaAnalysisResult javaAnalysis;
    private List<CSSAnalysisResult> cssAnalysis;

    public RepositoryAnalysisResult(
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<CSSAnalysisResult> cssAnalysis) {

        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;
        this.cssAnalysis = cssAnalysis;
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
}
