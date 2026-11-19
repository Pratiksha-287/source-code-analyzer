
package org.example.sourcecodeanalyser.service;

import com.github.javaparser.ast.CompilationUnit;
import org.example.sourcecodeanalyser.analyzer.JavaClassAnalyzer;
import org.example.sourcecodeanalyser.extractor.JavaSourceExtractor;
import org.example.sourcecodeanalyser.model.JavaClassInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class JavaAnalysisService {

    private final JavaSourceExtractor javaSourceExtractor;
    private final JavaClassAnalyzer javaClassAnalyzer;

    public JavaAnalysisService(
            JavaSourceExtractor javaSourceExtractor,
            JavaClassAnalyzer javaClassAnalyzer) {

        this.javaSourceExtractor = javaSourceExtractor;
        this.javaClassAnalyzer = javaClassAnalyzer;
    }

    public List<JavaClassInfo> analyzeJavaFiles(
            List<Path> javaFiles) throws IOException {

        List<JavaClassInfo> allClassInfos = new ArrayList<>();

        for (Path javaFile : javaFiles) {

            // Step 1: Parse Java source file
            CompilationUnit compilationUnit =
                    javaSourceExtractor.extract(javaFile);

            // Step 2: Analyze parsed Java structure
            List<JavaClassInfo> classInfos =
                    javaClassAnalyzer.analyze(
                            compilationUnit,
                            javaFile
                    );

            // Step 3: Collect extracted class information
            allClassInfos.addAll(classInfos);
        }

        return allClassInfos;
    }
}
