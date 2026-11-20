package org.example.sourcecodeanalyser.model.clientextension;

import java.nio.file.Path;

public class ClientExtensionInfo {

    private final String extensionName;
    private final Path extensionPath;
    private final Path configurationFile;

    public ClientExtensionInfo(
            String extensionName,
            Path extensionPath,
            Path configurationFile
    ) {
        this.extensionName = extensionName;
        this.extensionPath = extensionPath;
        this.configurationFile = configurationFile;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public Path getExtensionPath() {
        return extensionPath;
    }

    public Path getConfigurationFile() {
        return configurationFile;
    }
}