
package org.example.sourcecodeanalyser.analyzer.frontend;

import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.example.sourcecodeanalyser.model.frontend.ImportExportAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImportExportAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsImportsAndExports() throws Exception {

        String sourceCode = """
                import React from "react";
                import { useState, useEffect as effect } from "react";
                import axiosClient from "axios";
                import * as Router from "react-router-dom";

                export const API_URL = "/api", API_VERSION = "v1";

                export function greet(name) {
                    return "Hello " + name;
                }

                export default React;
                """;

        Path file = tempDir.resolve("App.jsx");
        Files.writeString(file, sourceCode);

        // Parse source code into AST
        Swc4jParser parser = new Swc4jParser();
        Swc4jParseOutput parseOutput = parser.parse(file);

        // Analyze AST
        ImportExportAnalyzer analyzer = new ImportExportAnalyzer();
        ImportExportAnalysis result = analyzer.analyze(parseOutput);

        // ---------------------------------
        // VERIFY IMPORTS
        // ---------------------------------

        assertEquals(4, result.imports().size());

        assertEquals("react", result.imports().get(0).source());
        assertEquals("react", result.imports().get(1).source());
        assertEquals("axios", result.imports().get(2).source());
        assertEquals("react-router-dom", result.imports().get(3).source());

        // ---------------------------------
        // VERIFY EXISTING STRING SPECIFIERS
        // ---------------------------------

        assertTrue(
                result.imports().get(0).specifiers()
                        .contains("default: React")
        );

        assertTrue(
                result.imports().get(1).specifiers()
                        .contains("named: useState as useState")
        );

        assertTrue(
                result.imports().get(1).specifiers()
                        .contains("named: useEffect as effect")
        );

        // ---------------------------------
        // VERIFY STRUCTURED DEFAULT IMPORT
        // ---------------------------------

        var reactImport = result.imports().get(0)
                .structuredSpecifiers().get(0);

        assertEquals("default", reactImport.kind());
        assertEquals("default", reactImport.importedName());
        assertEquals("React", reactImport.localName());

        // ---------------------------------
        // VERIFY STRUCTURED NAMED IMPORTS
        // ---------------------------------

        var namedImports = result.imports().get(1)
                .structuredSpecifiers();

        assertEquals(2, namedImports.size());

        assertEquals("named", namedImports.get(0).kind());
        assertEquals("useState", namedImports.get(0).importedName());
        assertEquals("useState", namedImports.get(0).localName());

        assertEquals("named", namedImports.get(1).kind());
        assertEquals("useEffect", namedImports.get(1).importedName());
        assertEquals("effect", namedImports.get(1).localName());

        // ---------------------------------
        // VERIFY RENAMED DEFAULT IMPORT
        // ---------------------------------

        var axiosImport = result.imports().get(2)
                .structuredSpecifiers().get(0);

        assertEquals("default", axiosImport.kind());
        assertEquals("default", axiosImport.importedName());
        assertEquals("axiosClient", axiosImport.localName());

        // ---------------------------------
        // VERIFY NAMESPACE IMPORT
        // ---------------------------------

        var namespaceImport = result.imports().get(3)
                .structuredSpecifiers().get(0);

        assertEquals("namespace", namespaceImport.kind());
        assertEquals("*", namespaceImport.importedName());
        assertEquals("Router", namespaceImport.localName());

        // ---------------------------------
        // VERIFY EXPORTS
        // ---------------------------------

        assertEquals(3, result.exports().size());

        assertTrue(
                result.exports().stream()
                        .anyMatch(export ->
                                export.kind().equals("DECLARATION")
                        )
        );

        assertTrue(
                result.exports().stream()
                        .anyMatch(export ->
                                export.kind().equals("DEFAULT_EXPRESSION")
                        )
        );

        assertTrue(
                result.exports().stream()
                        .anyMatch(export ->
                                export.names().contains("API_URL")
                        )
        );

        assertTrue(
                result.exports().stream()
                        .anyMatch(export ->
                                export.names().contains("API_VERSION")
                        )
        );

        assertTrue(
                result.exports().stream()
                        .anyMatch(export ->
                                export.names().contains("greet")
                        )
        );

        // ---------------------------------
        // PRINT RESULTS
        // ---------------------------------

        System.out.println("Structured imports:");

        result.imports().forEach(importInfo -> {
            System.out.println("Source: " + importInfo.source());

            importInfo.structuredSpecifiers()
                    .forEach(System.out::println);
        });

        System.out.println("Exports:");
        result.exports().forEach(System.out::println);
    }
}
