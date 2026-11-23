
package org.example.sourcecodeanalyser.model.clientextension;

import java.util.List;

public class ClientExtensionAnalysis {

    private String extensionName;
    private List<ClientExtensionConfiguration> configurations;
    private List<AssembleInfo> assemble;
    private List<FrontendFileAnalysis> frontendFiles;

    public ClientExtensionAnalysis(
            String extensionName,
            List<ClientExtensionConfiguration> configurations,
            List<AssembleInfo> assemble,
            List<FrontendFileAnalysis> frontendFiles
    ) {
        this.extensionName = extensionName;
        this.configurations = configurations;
        this.assemble = assemble;
        this.frontendFiles = frontendFiles;
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

    public List<FrontendFileAnalysis> getFrontendFiles() {
        return frontendFiles;
    }
}
