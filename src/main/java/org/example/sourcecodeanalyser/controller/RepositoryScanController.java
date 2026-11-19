
package org.example.sourcecodeanalyser.controller;

import org.example.sourcecodeanalyser.dto.RepositoryScanRequest;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;
import org.example.sourcecodeanalyser.service.RepositoryAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/analyzer")
public class RepositoryScanController {

    private final RepositoryAnalysisService repositoryAnalysisService;

    public RepositoryScanController(
            RepositoryAnalysisService repositoryAnalysisService) {

        this.repositoryAnalysisService = repositoryAnalysisService;
    }

    @PostMapping("/scan")
    public RepositoryAnalysisResult analyzeRepository(
            @RequestBody RepositoryScanRequest request)
            throws IOException {

        Path repositoryPath = Path.of(request.getRepositoryPath());

        return repositoryAnalysisService.analyzeRepository(
                repositoryPath
        );
    }
}
