
package org.example.sourcecodeanalyser.model.clientextension;

import java.util.Map;

public class ClientExtensionConfiguration {

    private String configurationKey;
    private String name;
    private String type;
    private Map<String, Object> properties;

    public ClientExtensionConfiguration(
            String configurationKey,
            String name,
            String type,
            Map<String, Object> properties
    ) {
        this.configurationKey = configurationKey;
        this.name = name;
        this.type = type;
        this.properties = properties;
    }

    public String getConfigurationKey() {
        return configurationKey;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
