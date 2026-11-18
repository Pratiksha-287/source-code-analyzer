
package org.example.sourcecodeanalyser.controller;

import org.example.sourcecodeanalyser.dto.RepositoryScanRequest;
import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.service.RepositoryScanService;

import org.springframework.web.bind.annotation.*;

        import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/analyzer")
public class RepositoryScanController {

    private final RepositoryScanService repositoryScanService;

    public RepositoryScanController(
            RepositoryScanService repositoryScanService) {

        this.repositoryScanService = repositoryScanService;
    }

    @PostMapping("/scan")
    public FileDiscoveryResult scanRepository(
            @RequestBody RepositoryScanRequest request)
            throws IOException {

        Path repositoryPath = Path.of(request.getRepositoryPath());

        return repositoryScanService.scanRepository(repositoryPath);
    }
}
