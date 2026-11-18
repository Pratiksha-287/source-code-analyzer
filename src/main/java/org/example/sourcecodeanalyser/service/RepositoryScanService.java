package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.scanner.FileDiscoveryService;
import org.example.sourcecodeanalyser.scanner.RepositoryScanner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class RepositoryScanService {

    private final RepositoryScanner repositoryScanner;
    private final FileDiscoveryService fileDiscoveryService;

    public RepositoryScanService(
            RepositoryScanner repositoryScanner,
            FileDiscoveryService fileDiscoveryService) {

        this.repositoryScanner = repositoryScanner;
        this.fileDiscoveryService = fileDiscoveryService;
    }

    public FileDiscoveryResult scanRepository(Path repositoryPath)
            throws IOException {

        // Step 1: Discover all files
        List<Path> discoveredFiles =
                repositoryScanner.scanRepository(repositoryPath);

        // Step 2: Classify discovered files
        Map<String, List<Path>> categorizedFiles =
                fileDiscoveryService.classifyFiles(discoveredFiles);

        // Step 3: Create structured result
        return new FileDiscoveryResult(
                repositoryPath,
                categorizedFiles
        );
    }
}
