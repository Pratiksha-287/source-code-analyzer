
package org.example.sourcecodeanalyser.extractor;

import org.example.sourcecodeanalyser.model.clientextension.AssembleInfo;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionConfiguration;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionExtractionResult;

@Component
public class ClientExtensionExtractor {

    public ClientExtensionExtractionResult extract(
            ClientExtensionInfo extensionInfo) {

        try (InputStream inputStream =
                     Files.newInputStream(extensionInfo.getConfigurationFile())) {

            Yaml yaml = new Yaml();

            Object loadedData = yaml.load(inputStream);

            if (!(loadedData instanceof Map<?, ?> yamlData)) {
                throw new IllegalArgumentException(
                        "Invalid or empty Client Extension YAML: "
                                + extensionInfo.getConfigurationFile()
                );
            }

            List<ClientExtensionConfiguration> configurations =
                    new ArrayList<>();

            List<AssembleInfo> assembleInfoList =
                    new ArrayList<>();

            for (Map.Entry<?, ?> entry : yamlData.entrySet()) {

                String configurationKey = entry.getKey().toString();
                Object configurationValue = entry.getValue();

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

            return new ClientExtensionExtractionResult(
                    configurations,
                    assembleInfoList
            );

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to extract Client Extension YAML: "
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
