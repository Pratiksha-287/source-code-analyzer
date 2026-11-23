
package org.example.sourcecodeanalyser;

import org.example.sourcecodeanalyser.frontend.discovery.FrontendFileDiscoveryService;
import org.example.sourcecodeanalyser.frontend.model.FrontendFileParseResult;
import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.example.sourcecodeanalyser.frontend.service.FrontendParsingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrontendPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void discoversAndParsesFrontendFiles() throws Exception {

        // Step 1: Create a sample Client Extension directory
        Path extensionDirectory = tempDir.resolve("sample-extension");

        Path sourceDirectory = extensionDirectory.resolve("src");

        Files.createDirectories(sourceDirectory);

        // Step 2: Create sample frontend files
        Files.writeString(
                sourceDirectory.resolve("App.jsx"),
                """
                const App = () => <h1>Hello React</h1>;
                export default App;
                """
        );

        Files.writeString(
                sourceDirectory.resolve("User.ts"),
                """
                interface User {
                    id: number;
                    name: string;
                }
                """
        );

        Files.writeString(
                sourceDirectory.resolve("utils.js"),
                """
                function greet(name) {
                    return "Hello " + name;
                }
                """
        );

        // Step 3: Discover frontend files
        FrontendFileDiscoveryService discoveryService =
                new FrontendFileDiscoveryService();

        List<Path> discoveredFiles =
                discoveryService.discoverFrontendFiles(extensionDirectory);

        assertEquals(3, discoveredFiles.size());

        // Step 4: Parse discovered files
        FrontendParsingService parsingService =
                new FrontendParsingService(new Swc4jParser());

        List<FrontendFileParseResult> results =
                parsingService.parseFiles(discoveredFiles);

        // Step 5: Verify results
        assertEquals(3, results.size());

        assertTrue(
                results.stream().allMatch(
                        FrontendFileParseResult::isSuccessful
                )
        );

        assertTrue(
                results.stream().allMatch(
                        result -> result.parseOutput().getProgram() != null
                )
        );

        System.out.println("Discovered files: " + discoveredFiles.size());
        System.out.println("Parsed files: " + results.size());

        results.forEach(result ->
                System.out.println(
                        result.filePath().getFileName()
                                + " -> "
                                + (result.isSuccessful()
                                ? "SUCCESS"
                                : "FAILED")
                )
        );
    }
}
