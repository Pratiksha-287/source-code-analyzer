
package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.example.sourcecodeanalyser.model.JavaClassInfo;
import org.example.sourcecodeanalyser.model.JavaMethodInfo;
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

        for (TypeDeclaration<?> type : compilationUnit.getTypes()) {

            String classType = type.getClass().getSimpleName();

            List<String> annotations = type.getAnnotations()
                    .stream()
                    .map(annotation ->
                            annotation.getNameAsString())
                    .toList();

            // Extract methods belonging to this type
            List<JavaMethodInfo> methods = new ArrayList<>();

            for (MethodDeclaration method : type.getMethods()) {

                List<String> parameters = method.getParameters()
                        .stream()
                        .map(parameter ->
                                parameter.getTypeAsString()
                                        + " " + parameter.getNameAsString())
                        .toList();

                List<String> methodAnnotations =
                        method.getAnnotations()
                                .stream()
                                .map(annotation ->
                                        annotation.getNameAsString())
                                .toList();

                JavaMethodInfo methodInfo = new JavaMethodInfo(
                        method.getNameAsString(),
                        method.getTypeAsString(),
                        parameters,
                        methodAnnotations
                );

                methods.add(methodInfo);
            }

            JavaClassInfo classInfo = new JavaClassInfo(
                    packageName,
                    type.getNameAsString(),
                    classType,
                    annotations,
                    javaFile.toString(),
                    methods
            );

            classInfos.add(classInfo);
        }

        return classInfos;
    }
}
