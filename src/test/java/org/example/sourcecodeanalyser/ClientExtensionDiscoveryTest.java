
package org.example.sourcecodeanalyser;

import org.example.sourcecodeanalyser.discovery.ClientExtensionDiscovery;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientExtensionDiscoveryTest {

    @TempDir
    Path tempDir;

    private final ClientExtensionDiscovery discovery =
            new ClientExtensionDiscovery();

    @Test
    void shouldDiscoverClientExtensionsRecursively() throws IOException {

        // Arrange: create a sample client-extensions directory structure.
        Path clientExtensions = tempDir.resolve("client-extensions");

        Path accountCreation =
                clientExtensions.resolve("account-creation");

        Path govconDashboard =
                clientExtensions.resolve("govcon/govcon-dashboard");

        Path marketingFrontend =
                clientExtensions.resolve(
                        "marketing-accelerator/nl-marketing-frontend"
                );

        Files.createDirectories(accountCreation);
        Files.createDirectories(govconDashboard);
        Files.createDirectories(marketingFrontend);

        Files.createFile(
                accountCreation.resolve("client-extension.yaml")
        );

        Files.createFile(
                govconDashboard.resolve("client-extension.yaml")
        );

        Files.createFile(
                marketingFrontend.resolve("client-extension.yaml")
        );

        // Act: discover extensions recursively.
        List<ClientExtensionInfo> extensions =
                discovery.discoverClientExtensions(clientExtensions);

        // Assert: all three extensions should be discovered.
        assertEquals(3, extensions.size());

        assertEquals(
                List.of(
                        "account-creation",
                        "govcon-dashboard",
                        "nl-marketing-frontend"
                ),
                extensions.stream()
                        .map(ClientExtensionInfo::getExtensionName)
                        .toList()
        );

        // Verify that every result points to a real directory
        // and an existing configuration file.
        for (ClientExtensionInfo extension : extensions) {
            assertTrue(Files.isDirectory(extension.getExtensionPath()));
            assertTrue(Files.isRegularFile(
                    extension.getConfigurationFile()
            ));

            assertEquals(
                    extension.getExtensionPath().resolve(
                            "client-extension.yaml"
                    ),
                    extension.getConfigurationFile()
            );
        }
    }
}
