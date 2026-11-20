
package org.example.sourcecodeanalyser.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import org.example.sourcecodeanalyser.analyzer.JavaClassAnalyzer;
import org.example.sourcecodeanalyser.extractor.JavaSourceExtractor;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.JavaClassInfo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public JavaAnalysisResult analyzeJavaFiles(
            List<Path> javaFiles) throws IOException {

        List<JavaClassInfo> allClassInfos = new ArrayList<>();

        // Store parsed Java files so that we don't parse them twice.
        Map<Path, CompilationUnit> parsedFiles = new HashMap<>();

        // Store fully qualified names of classes found in this repository.
        Set<String> projectTypes = new HashSet<>();

        // PASS 1: Parse all files and collect project class names.
        for (Path javaFile : javaFiles) {

            CompilationUnit compilationUnit =
                    javaSourceExtractor.extract(javaFile);

            parsedFiles.put(javaFile, compilationUnit);

            String packageName = compilationUnit
                    .getPackageDeclaration()
                    .map(packageDeclaration ->
                            packageDeclaration.getNameAsString())
                    .orElse("");

            for (TypeDeclaration<?> type : compilationUnit.getTypes()) {

                String typeName = type.getNameAsString();

                String fullyQualifiedName = packageName.isEmpty()
                        ? typeName
                        : packageName + "." + typeName;

                projectTypes.add(fullyQualifiedName);
            }
        }

        // PASS 2: Analyze files using the complete project class index.
        for (Path javaFile : javaFiles) {

            CompilationUnit compilationUnit = parsedFiles.get(javaFile);

            List<JavaClassInfo> classInfos =
                    javaClassAnalyzer.analyze(
                            compilationUnit,
                            javaFile,
                            projectTypes
                    );

            allClassInfos.addAll(classInfos);
        }

        // Create the final Java analysis result.
        return new JavaAnalysisResult(
                javaFiles.size(),
                allClassInfos
        );
    }
}
