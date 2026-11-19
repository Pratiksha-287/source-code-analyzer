
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class RepositoryAnalysisService {

    private final RepositoryScanService repositoryScanService;
    private final JavaAnalysisService javaAnalysisService;

    public RepositoryAnalysisService(
            RepositoryScanService repositoryScanService,
            JavaAnalysisService javaAnalysisService) {

        this.repositoryScanService = repositoryScanService;
        this.javaAnalysisService = javaAnalysisService;
    }

    public RepositoryAnalysisResult analyzeRepository(
            Path repositoryPath) throws IOException {

        // Step 1: Scan the repository
        FileDiscoveryResult fileDiscovery =
                repositoryScanService.scanRepository(repositoryPath);

        // Step 2: Get all discovered Java files
        List<Path> javaFiles = fileDiscovery
                .getCategorizedFiles()
                .getOrDefault("JAVA", List.of());

        // Step 3: Analyze Java files
        JavaAnalysisResult javaAnalysis =
                javaAnalysisService.analyzeJavaFiles(javaFiles);

        // Step 4: Combine both results
        return new RepositoryAnalysisResult(
                fileDiscovery,
                javaAnalysis
        );
    }
}
