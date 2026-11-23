
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.analyzer.frontend.FunctionAnalyzer;
import org.example.sourcecodeanalyser.analyzer.frontend.ImportExportAnalyzer;
import org.example.sourcecodeanalyser.analyzer.frontend.ReactAnalyzer;
import org.example.sourcecodeanalyser.frontend.discovery.FrontendFileDiscoveryService;
import org.example.sourcecodeanalyser.frontend.model.FrontendFileParseResult;
import org.example.sourcecodeanalyser.model.clientextension.FrontendFileAnalysis;
import org.example.sourcecodeanalyser.model.frontend.FunctionAnalysis;
import org.example.sourcecodeanalyser.model.frontend.ImportExportAnalysis;
import org.example.sourcecodeanalyser.model.frontend.ReactAnalysis;
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

    public List<FrontendFileAnalysis> analyzeExtension(Path extensionPath) {

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

        ImportExportAnalysis importExport =
                importExportAnalyzer.analyze(parseOutput);

        FunctionAnalysis functions =
                functionAnalyzer.analyze(parseOutput);

        ReactAnalysis react =
                reactAnalyzer.analyze(parseOutput);

        return new FrontendFileAnalysis(
                relativePath,
                true,
                null,
                hasImportExportData(importExport) ? importExport : null,
                hasFunctionData(functions) ? functions : null,
                hasReactData(react) ? react : null
        );
    }

    private boolean hasImportExportData(ImportExportAnalysis analysis) {
        return analysis != null
                && ((analysis.imports() != null && !analysis.imports().isEmpty())
                || (analysis.exports() != null && !analysis.exports().isEmpty()));
    }

    private boolean hasFunctionData(FunctionAnalysis analysis) {
        return analysis != null
                && analysis.functions() != null
                && !analysis.functions().isEmpty();
    }

    private boolean hasReactData(ReactAnalysis analysis) {
        return analysis != null
                && (
                !analysis.components().isEmpty()
                        || !analysis.hooks().isEmpty()
                        || !analysis.apiInteractions().isEmpty()
                        || !analysis.axiosInstances().isEmpty()
                        || !analysis.customHooks().isEmpty()
                        || !analysis.routes().isEmpty()
                        || !analysis.navigations().isEmpty()
                        || !analysis.routerHooks().isEmpty()
                        || !analysis.functions().isEmpty()
        );
    }
}
