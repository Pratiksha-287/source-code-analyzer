
package org.example.sourcecodeanalyser.analyzer.clientextension;

import org.example.sourcecodeanalyser.extractor.ClientExtensionExtractor;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionExtractionResult;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.springframework.stereotype.Service;

@Service
public class ClientExtensionAnalyzer {

    private final ClientExtensionExtractor clientExtensionExtractor;

    public ClientExtensionAnalyzer(
            ClientExtensionExtractor clientExtensionExtractor) {

        this.clientExtensionExtractor = clientExtensionExtractor;
    }

    public ClientExtensionAnalysis analyze(
            ClientExtensionInfo extensionInfo) {

        // Step 1: Extract information from YAML
        ClientExtensionExtractionResult extractionResult =
                clientExtensionExtractor.extract(extensionInfo);

        // Step 2: Create structured analysis result
        return new ClientExtensionAnalysis(
                extensionInfo.getExtensionName(),
                extractionResult.getConfigurations(),
                extractionResult.getAssemble()
        );
    }
}
