
package org.example.sourcecodeanalyser;

import org.example.sourcecodeanalyser.analyzer.python.PythonFileAnalyzer;
import org.example.sourcecodeanalyser.model.python.PythonFileAnalysis;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PythonFileAnalyzerTest {

    @Test
    void shouldAnalyzePythonFile() throws Exception {

        PythonFileAnalyzer analyzer = new PythonFileAnalyzer();

        PythonFileAnalysis result = analyzer.analyze(
                Path.of("test-data/python/sample.py")
        );

        assertNotNull(result);

        System.out.println("\n========== PYTHON ANALYSIS ==========");
        System.out.println(result);

        System.out.println("\nImports: " + result.imports());
        System.out.println("Classes: " + result.classes());
        System.out.println("Functions: " + result.functions());
        System.out.println("Variables: " + result.variables());
        System.out.println("Exports: " + result.exports());
        System.out.println("Relationships: " + result.relationships());
        System.out.println("Control flow: " + result.controlFlow());
        System.out.println("Diagnostics: " + result.diagnostics());

        // Imports
        assertEquals(4, result.imports().size());

        // Classes
        assertEquals(5, result.classes().size());

        assertTrue(result.classes().stream()
                .anyMatch(c -> c.name().equals("Employee")));

        assertTrue(result.classes().stream()
                .anyMatch(c -> c.name().equals("PublicClass")));

        PythonFileAnalysis.PythonClassInfo employee =
                result.classes().stream()
                        .filter(c -> c.name().equals("Employee"))
                        .findFirst()
                        .orElseThrow();

        assertEquals(2, employee.methods().size());

        // Functions
        assertEquals(10, result.functions().size());

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("greet")));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("calculate")));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("create_user")));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("public_function")));

        assertTrue(result.functions().stream()
                .anyMatch(f -> f.name().equals("_private_function")));

        // Async functions
        PythonFileAnalysis.PythonFunctionInfo fetchUser =
                result.functions().stream()
                        .filter(f -> f.name().equals("fetch_user"))
                        .findFirst()
                        .orElseThrow();

        PythonFileAnalysis.PythonFunctionInfo main =
                result.functions().stream()
                        .filter(f -> f.name().equals("main"))
                        .findFirst()
                        .orElseThrow();

        assertTrue(fetchUser.asynchronous());
        assertTrue(main.asynchronous());

        // Normal functions should not be asynchronous
        PythonFileAnalysis.PythonFunctionInfo calculate =
                result.functions().stream()
                        .filter(f -> f.name().equals("calculate"))
                        .findFirst()
                        .orElseThrow();

        assertFalse(calculate.asynchronous());

        assertEquals(4, result.lambdas().size());

        assertEquals("x * x",
                result.lambdas().get(0).expression());
        assertEquals("a + b",
                result.lambdas().get(1).expression());
        assertEquals("x * factor", result.lambdas().get(2).expression());
        assertEquals("n * 2", result.lambdas().get(3).expression());

        assertEquals("factor",
                result.lambdas().get(2).parameters().get(1).name());

        assertEquals("2",
                result.lambdas().get(2).parameters().get(1).defaultValue());

        // Explicit exports from __all__
        assertEquals(3, result.exports().size());

        assertTrue(result.exports().stream()
                .anyMatch(e -> e.name().equals("greet")));

        assertTrue(result.exports().stream()
                .anyMatch(e -> e.name().equals("Employee")));

        assertTrue(result.exports().stream()
                .anyMatch(e -> e.name().equals("public_variable")));

        assertFalse(result.exports().stream()
                .anyMatch(e -> e.name().equals("public_function")));

        assertFalse(result.exports().stream()
                .anyMatch(e -> e.name().equals("_private_function")));

        assertFalse(result.exports().stream()
                .anyMatch(e -> e.name().equals("_private_variable")));

        // Diagnostics
        assertTrue(result.diagnostics().isEmpty());
    }
    @Test
    void shouldAnalyzeAdvancedPythonFeatures() throws Exception {
        PythonFileAnalyzer analyzer = new PythonFileAnalyzer();
        PythonFileAnalysis result = analyzer.analyze(
                Path.of("test-data/python/advanced_features.py")
        );

        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());

        // Relative, wildcard and aliased imports.
        assertTrue(result.imports().stream().anyMatch(i ->
                i.relativeImport() && i.relativeLevel() == 1
                        && "helper".equals(i.importedName())));
        assertTrue(result.imports().stream().anyMatch(i ->
                i.relativeImport() && i.relativeLevel() == 2
                        && "*".equals(i.importedName())));
        assertTrue(result.imports().stream().anyMatch(i ->
                "item".equals(i.importedName())
                        && "renamed_item".equals(i.alias())));

        PythonFileAnalysis.PythonFunctionInfo calculate =
                result.functions().stream()
                        .filter(f -> f.name().equals("calculate"))
                        .findFirst().orElseThrow();
        assertEquals(List.of("@traced", "@staticmethod"), calculate.decorators());
        assertEquals("float", calculate.returnAnnotation());
        assertEquals("int", calculate.parameters().get(0).annotation());
        assertEquals("float", calculate.parameters().get(1).annotation());
        assertEquals("2.0", calculate.parameters().get(1).defaultValue());
        assertEquals(1, calculate.nestedFunctions().size());

        assertTrue(result.variables().stream().anyMatch(v ->
                v.name().equals("local_value")
                        && v.scope().equals("FUNCTION:calculate")
                        && "int".equals(v.annotation())));
        assertTrue(result.variables().stream().anyMatch(v ->
                v.name().equals("nested_value")
                        && v.scope().equals("FUNCTION:calculate.nested")));

        assertTrue(result.comprehensions().stream().anyMatch(c ->
                c.kind().equals("LIST_COMPREHENSION")));
        assertTrue(result.comprehensions().stream().anyMatch(c ->
                c.kind().equals("DICTIONARY_COMPREHENSION")));
        assertTrue(result.comprehensions().stream().anyMatch(c ->
                c.kind().equals("SET_COMPREHENSION")));
        assertTrue(result.comprehensions().stream().anyMatch(c ->
                c.kind().equals("GENERATOR_EXPRESSION")));
        assertTrue(result.comprehensions().stream().anyMatch(c ->
                c.kind().equals("LIST_COMPREHENSION")
                        && c.iterationClause().contains("for x in rows")
                        && c.iterationClause().contains("for y in columns")));

        // The complete analysis record must remain JSON serializable.
        String json = new tools.jackson.databind.ObjectMapper()
                .writeValueAsString(result);
        assertTrue(json.contains("comprehensions"));
        assertTrue(json.contains("GENERATOR_EXPRESSION"));
    }

}
