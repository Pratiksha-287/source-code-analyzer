package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.example.sourcecodeanalyser.model.JavaClassInfo;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaClassAnalyzer {

    public List<JavaClassInfo> analyze(
            CompilationUnit compilationUnit,
            Path javaFile) {

        List<JavaClassInfo> classInfos = new ArrayList<>();

        String packageName = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration ->
                        packageDeclaration.getNameAsString())
                .orElse("");

        for (TypeDeclaration<?> type :
                compilationUnit.getTypes()) {

            String classType = type.getClass().getSimpleName();

            List<String> annotations = type.getAnnotations()
                    .stream()
                    .map(annotation ->
                            annotation.getNameAsString())
                    .toList();

            JavaClassInfo classInfo = new JavaClassInfo(
                    packageName,
                    type.getNameAsString(),
                    classType,
                    annotations,
                    javaFile.toString()
            );

            classInfos.add(classInfo);
        }

        return classInfos;
    }
}
