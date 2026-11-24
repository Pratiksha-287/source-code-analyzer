package org.example.sourcecodeanalyser.model.clientextension;

import java.util.List;

public class ClientExtensionExtractionResult {

    private final List<ClientExtensionConfiguration> configurations;
    private final List<AssembleInfo> assemble;

    public ClientExtensionExtractionResult(
            List<ClientExtensionConfiguration> configurations,
            List<AssembleInfo> assemble) {

        this.configurations = configurations;
        this.assemble = assemble;
    }

    public List<ClientExtensionConfiguration> getConfigurations() {
        return configurations;
    }

    public List<AssembleInfo> getAssemble() {
        return assemble;
    }
}