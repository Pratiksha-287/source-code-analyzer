
package org.example.sourcecodeanalyser.analyzer.clientextension;

import org.example.sourcecodeanalyser.model.clientextension.AssembleInfo;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionConfiguration;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientExtensionAnalyzer {

    public ClientExtensionAnalysis analyze(ClientExtensionInfo extensionInfo) {

        try (InputStream inputStream =
                     Files.newInputStream(extensionInfo.getConfigurationFile())) {

            Yaml yaml = new Yaml();

            Map<String, Object> yamlData = yaml.load(inputStream);

            if (yamlData == null) {
                throw new IllegalArgumentException(
                        "Empty Client Extension YAML file"
                );
            }

            List<ClientExtensionConfiguration> configurations =
                    new ArrayList<>();

            List<AssembleInfo> assembleInfoList =
                    new ArrayList<>();

            for (Map.Entry<String, Object> entry : yamlData.entrySet()) {

                String configurationKey = entry.getKey();
                Object configurationValue = entry.getValue();

                // Handle assemble separately.
                if ("assemble".equals(configurationKey)) {

                    if (configurationValue instanceof List<?> assembleList) {

                        for (Object item : assembleList) {

                            if (item instanceof Map<?, ?> assembleMap) {

                                String from = getString(assembleMap, "from");
                                String into = getString(assembleMap, "into");

                                assembleInfoList.add(
                                        new AssembleInfo(from, into)
                                );
                            }
                        }
                    }

                    continue;
                }

                // Process every other configuration.
                if (configurationValue instanceof Map<?, ?> rawMap) {

                    Map<String, Object> configurationMap =
                            new LinkedHashMap<>();

                    for (Map.Entry<?, ?> property : rawMap.entrySet()) {

                        configurationMap.put(
                                property.getKey().toString(),
                                property.getValue()
                        );
                    }

                    String name = getString(configurationMap, "name");
                    String type = getString(configurationMap, "type");

                    configurations.add(
                            new ClientExtensionConfiguration(
                                    configurationKey,
                                    name,
                                    type,
                                    configurationMap
                            )
                    );
                }
            }

            return new ClientExtensionAnalysis(
                    extensionInfo.getExtensionName(),
                    configurations,
                    assembleInfoList
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to read Client Extension YAML: "
                            + extensionInfo.getConfigurationFile(),
                    exception
            );
        }
    }

    private String getString(Map<?, ?> map, String key) {

        Object value = map.get(key);

        return value == null ? null : value.toString();
    }
}
