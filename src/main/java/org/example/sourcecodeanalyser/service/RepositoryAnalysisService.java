
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.extractor.CSSSourceExtractor;
import org.example.sourcecodeanalyser.model.CSSAnalysisResult;
import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepositoryAnalysisService {

    private final RepositoryScanService repositoryScanService;
    private final JavaAnalysisService javaAnalysisService;
    private final CSSSourceExtractor cssSourceExtractor;

    public RepositoryAnalysisService(
            RepositoryScanService repositoryScanService,
            JavaAnalysisService javaAnalysisService,
            CSSSourceExtractor cssSourceExtractor) {

        this.repositoryScanService = repositoryScanService;
        this.javaAnalysisService = javaAnalysisService;
        this.cssSourceExtractor = cssSourceExtractor;
    }

    public RepositoryAnalysisResult analyzeRepository(
            Path repositoryPath) throws IOException {

        // Discover repository files.
        FileDiscoveryResult fileDiscovery =
                repositoryScanService.scanRepository(repositoryPath);

        // Analyze Java files.
        List<Path> javaFiles =
                fileDiscovery.getCategorizedFiles()
                        .getOrDefault("JAVA", List.of());

        JavaAnalysisResult javaAnalysis =
                javaAnalysisService.analyzeJavaFiles(javaFiles);

        // Analyze CSS files.
        List<Path> cssFiles =
                fileDiscovery.getCategorizedFiles()
                        .getOrDefault("CSS", List.of());

        List<CSSAnalysisResult> cssAnalysis = new ArrayList<>();

        for (Path cssFile : cssFiles) {
            cssAnalysis.add(
                    cssSourceExtractor.extract(cssFile)
            );
        }

        // Return combined analysis.
        return new RepositoryAnalysisResult(
                fileDiscovery,
                javaAnalysis,
                cssAnalysis
        );
    }
}
