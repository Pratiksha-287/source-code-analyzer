
package org.example.sourcecodeanalyser.model.clientextension;

import java.util.List;

public class ClientExtensionAnalysis {

    private String extensionName;
    private List<ClientExtensionConfiguration> configurations;
    private List<AssembleInfo> assemble;

    public ClientExtensionAnalysis(
            String extensionName,
            List<ClientExtensionConfiguration> configurations,
            List<AssembleInfo> assemble
    ) {
        this.extensionName = extensionName;
        this.configurations = configurations;
        this.assemble = assemble;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public List<ClientExtensionConfiguration> getConfigurations() {
        return configurations;
    }

    public List<AssembleInfo> getAssemble() {
        return assemble;
    }
}
