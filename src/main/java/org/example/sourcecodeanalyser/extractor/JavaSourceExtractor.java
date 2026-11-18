
package org.example.sourcecodeanalyser.extractor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class JavaSourceExtractor {

    public CompilationUnit extract(Path javaFile)
            throws IOException {

        // Create JavaParser instance
        JavaParser javaParser = new JavaParser();

        // Parse the Java source file
        ParseResult<CompilationUnit> parseResult =
                javaParser.parse(javaFile);

        // Return the parsed Java structure
        return parseResult.getResult()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unable to parse Java file: " + javaFile
                                        + ". Problems: " + parseResult.getProblems()
                        )
                );
    }
}
