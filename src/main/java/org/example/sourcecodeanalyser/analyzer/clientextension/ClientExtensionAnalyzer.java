
package org.example.sourcecodeanalyser.analyzer.clientextension;

import org.example.sourcecodeanalyser.extractor.ClientExtensionExtractor;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionExtractionResult;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.example.sourcecodeanalyser.service.FrontendFileAnalysisService;
import org.springframework.stereotype.Service;

@Service
public class ClientExtensionAnalyzer {

    private final ClientExtensionExtractor clientExtensionExtractor;
    private final FrontendFileAnalysisService frontendFileAnalysisService;

    public ClientExtensionAnalyzer(
            ClientExtensionExtractor clientExtensionExtractor,
            FrontendFileAnalysisService frontendFileAnalysisService) {

        this.clientExtensionExtractor = clientExtensionExtractor;
        this.frontendFileAnalysisService = frontendFileAnalysisService;
    }

    public ClientExtensionAnalysis analyze(
            ClientExtensionInfo extensionInfo) {

        // Step 1: Extract YAML configuration information
        ClientExtensionExtractionResult extractionResult =
                clientExtensionExtractor.extract(extensionInfo);

        // Step 2: Analyze frontend source files
        var frontendFiles =
                frontendFileAnalysisService.analyzeExtension(
                        extensionInfo.getExtensionPath()
                );

        // Step 3: Combine YAML and frontend analysis results
        return new ClientExtensionAnalysis(
                extensionInfo.getExtensionName(),
                extractionResult.getConfigurations(),
                extractionResult.getAssemble(),
                frontendFiles
        );
    }
}
