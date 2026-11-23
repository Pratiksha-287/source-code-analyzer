
package org.example.sourcecodeanalyser;

import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.example.sourcecodeanalyser.analyzer.frontend.FunctionAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FunctionAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAnalyzeDifferentFunctionTypes() throws Exception {

        String sourceCode = """
                function greet(name, age) {
                    return "Hello " + name;
                }

                const add = (a, b) => a + b;

                async function fetchUsers() {
                    return [];
                }

                const multiply = function(a, b) {
                    return a * b;
                };

                const load = async () => [];
                """;

        Path file = tempDir.resolve("functions.js");
        Files.writeString(file, sourceCode);

        Swc4jParser parser = new Swc4jParser();

        var parseOutput = parser.parse(file);

        FunctionAnalyzer analyzer = new FunctionAnalyzer();

        var result = analyzer.analyze(parseOutput);

        result.functions().forEach(System.out::println);

        assertEquals(5, result.functions().size());

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("greet")
                        && f.kind().equals("DECLARATION")
                        && f.parameters().equals(
                        java.util.List.of("name", "age"))
                        && !f.async()));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("add")
                        && f.kind().equals("ARROW")
                        && f.parameters().equals(
                        java.util.List.of("a", "b"))));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("fetchUsers")
                        && f.async()));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("multiply")
                        && f.kind().equals("EXPRESSION")));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("load")
                        && f.kind().equals("ARROW")
                        && f.async()));
    }
}
