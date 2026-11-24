
package org.example.sourcecodeanalyser.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import org.example.sourcecodeanalyser.analyzer.JavaClassAnalyzer;
import org.example.sourcecodeanalyser.extractor.JavaSourceExtractor;
import org.example.sourcecodeanalyser.model.FileAnalysisMetadata;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.JavaClassInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JavaAnalysisService {

    private static final Logger log =
            LoggerFactory.getLogger(JavaAnalysisService.class);

    private final JavaSourceExtractor javaSourceExtractor;
    private final JavaClassAnalyzer javaClassAnalyzer;

    public JavaAnalysisService(
            JavaSourceExtractor javaSourceExtractor,
            JavaClassAnalyzer javaClassAnalyzer) {

        this.javaSourceExtractor = javaSourceExtractor;
        this.javaClassAnalyzer = javaClassAnalyzer;
    }

    public JavaAnalysisResult analyzeJavaFiles(
            List<Path> javaFiles,
            Path repositoryRoot) {

        List<JavaClassInfo> allClassInfos = new ArrayList<>();

        Map<Path, CompilationUnit> parsedFiles =
                new LinkedHashMap<>();

        Set<String> projectTypes = new HashSet<>();

        Path normalizedRoot =
                repositoryRoot.toAbsolutePath().normalize();

        // PASS 1: Parse files and collect project class names.
        for (Path javaFile : javaFiles) {

            try {
                CompilationUnit compilationUnit =
                        javaSourceExtractor.extract(javaFile);

                parsedFiles.put(javaFile, compilationUnit);

                String packageName = compilationUnit
                        .getPackageDeclaration()
                        .map(packageDeclaration ->
                                packageDeclaration.getNameAsString())
                        .orElse("");

                for (TypeDeclaration<?> type :
                        compilationUnit.getTypes()) {

                    String typeName = type.getNameAsString();

                    String fullyQualifiedName = packageName.isEmpty()
                            ? typeName
                            : packageName + "." + typeName;

                    projectTypes.add(fullyQualifiedName);
                }

            } catch (Exception e) {

                log.warn(
                        "Skipping Java file {} because parsing failed",
                        javaFile,
                        e
                );
            }
        }

        int successfullyAnalyzedFiles = 0;

        // PASS 2: Analyze successfully parsed files.
        for (Map.Entry<Path, CompilationUnit> entry :
                parsedFiles.entrySet()) {

            Path javaFile = entry.getKey();
            CompilationUnit compilationUnit = entry.getValue();

            try {

                Path normalizedFile =
                        javaFile.toAbsolutePath().normalize();

                String relativePath = normalizedRoot
                        .relativize(normalizedFile)
                        .toString()
                        .replace('\\', '/');

                FileAnalysisMetadata fileMetadata =
                        new FileAnalysisMetadata(
                                normalizedFile.getFileName().toString(),
                                relativePath,
                                "JAVA"
                        );

                List<JavaClassInfo> classInfos =
                        javaClassAnalyzer.analyze(
                                compilationUnit,
                                javaFile,
                                fileMetadata,
                                projectTypes
                        );

                allClassInfos.addAll(classInfos);

                successfullyAnalyzedFiles++;

            } catch (Exception e) {

                log.warn(
                        "Skipping Java file {} because analysis failed",
                        javaFile,
                        e
                );
            }
        }

        return new JavaAnalysisResult(
                successfullyAnalyzedFiles,
                allClassInfos
        );
    }
}
