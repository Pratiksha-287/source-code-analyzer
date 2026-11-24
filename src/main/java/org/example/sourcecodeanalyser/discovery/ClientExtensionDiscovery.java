package org.example.sourcecodeanalyser.discovery;

import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ClientExtensionDiscovery {

    public List<ClientExtensionInfo> discoverClientExtensions(
            Path clientExtensionsDirectory
    ) {

        if (clientExtensionsDirectory == null) {
            return List.of();
        }

        if (!Files.isDirectory(clientExtensionsDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid client extensions directory: "
                            + clientExtensionsDirectory
            );
        }

        try (Stream<Path> paths = Files.walk(clientExtensionsDirectory)) {

            return paths
                    .filter(Files::isDirectory)
                    .map(directory -> {
                        Path yamlFile =
                                directory.resolve("client-extension.yaml");

                        if (Files.isRegularFile(yamlFile)) {
                            return new ClientExtensionInfo(
                                    directory.getFileName().toString(),
                                    directory,
                                    yamlFile
                            );
                        }

                        return null;
                    })
                    .filter(extension -> extension != null)
                    .sorted(Comparator.comparing(
                            ClientExtensionInfo::getExtensionName
                    ))
                    .toList();

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to discover client extensions in: "
                            + clientExtensionsDirectory,
                    exception
            );
        }
    }
}