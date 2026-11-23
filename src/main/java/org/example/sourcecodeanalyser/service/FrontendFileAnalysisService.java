
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.analyzer.frontend.FunctionAnalyzer;
import org.example.sourcecodeanalyser.analyzer.frontend.ImportExportAnalyzer;
import org.example.sourcecodeanalyser.analyzer.frontend.ReactAnalyzer;
import org.example.sourcecodeanalyser.frontend.discovery.FrontendFileDiscoveryService;
import org.example.sourcecodeanalyser.frontend.model.FrontendFileParseResult;
import org.example.sourcecodeanalyser.model.clientextension.FrontendFileAnalysis;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class FrontendFileAnalysisService {

    private final FrontendFileDiscoveryService discoveryService;
    private final org.example.sourcecodeanalyser.frontend.service.FrontendParsingService parsingService;
    private final ImportExportAnalyzer importExportAnalyzer;
    private final FunctionAnalyzer functionAnalyzer;
    private final ReactAnalyzer reactAnalyzer;

    public FrontendFileAnalysisService(
            FrontendFileDiscoveryService discoveryService,
            org.example.sourcecodeanalyser.frontend.service.FrontendParsingService parsingService,
            ImportExportAnalyzer importExportAnalyzer,
            FunctionAnalyzer functionAnalyzer,
            ReactAnalyzer reactAnalyzer) {

        this.discoveryService = discoveryService;
        this.parsingService = parsingService;
        this.importExportAnalyzer = importExportAnalyzer;
        this.functionAnalyzer = functionAnalyzer;
        this.reactAnalyzer = reactAnalyzer;
    }

    public List<FrontendFileAnalysis> analyzeExtension(
            Path extensionPath) {

        List<Path> frontendFiles =
                discoveryService.discoverFrontendFiles(extensionPath);

        List<FrontendFileParseResult> parseResults =
                parsingService.parseFiles(frontendFiles);

        return parseResults.stream()
                .map(result -> toAnalysis(extensionPath, result))
                .toList();
    }

    private FrontendFileAnalysis toAnalysis(
            Path extensionPath,
            FrontendFileParseResult result) {

        String relativePath = extensionPath
                .toAbsolutePath()
                .normalize()
                .relativize(
                        result.filePath()
                                .toAbsolutePath()
                                .normalize()
                )
                .toString()
                .replace('\\', '/');

        if (!result.isSuccessful()) {
            return new FrontendFileAnalysis(
                    relativePath,
                    false,
                    result.errorMessage(),
                    null,
                    null,
                    null
            );
        }

        var parseOutput = result.parseOutput();

        return new FrontendFileAnalysis(
                relativePath,
                true,
                null,
                importExportAnalyzer.analyze(parseOutput),
                functionAnalyzer.analyze(parseOutput),
                reactAnalyzer.analyze(parseOutput)
        );
    }
}
