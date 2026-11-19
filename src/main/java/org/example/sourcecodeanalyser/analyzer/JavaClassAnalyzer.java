
package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.example.sourcecodeanalyser.model.JavaClassInfo;
import org.example.sourcecodeanalyser.model.JavaConstructorInfo;
import org.example.sourcecodeanalyser.model.JavaFieldInfo;
import org.example.sourcecodeanalyser.model.JavaMethodInfo;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;

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

            String classType = determineClassType(type);

            List<String> annotations = type.getAnnotations()
                    .stream()
                    .map(annotation ->
                            annotation.getNameAsString())
                    .toList();

            // Extract methods
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

                methods.add(new JavaMethodInfo(
                        method.getNameAsString(),
                        method.getTypeAsString(),
                        parameters,
                        methodAnnotations
                ));
            }

            // Extract fields
            List<JavaFieldInfo> fields = new ArrayList<>();

            for (FieldDeclaration field : type.getFields()) {

                String accessModifier = field.getAccessSpecifier()
                        .asString();

                List<String> fieldAnnotations =
                        field.getAnnotations()
                                .stream()
                                .map(annotation ->
                                        annotation.getNameAsString())
                                .toList();

                field.getVariables().forEach(variable -> {

                    fields.add(new JavaFieldInfo(
                            variable.getNameAsString(),
                            variable.getTypeAsString(),
                            accessModifier,
                            fieldAnnotations
                    ));
                });
            }

            // Extract constructors
            List<JavaConstructorInfo> constructors =
                    new ArrayList<>();

            for (ConstructorDeclaration constructor :
                    type.getMembers().stream()
                            .filter(ConstructorDeclaration.class::isInstance)
                            .map(ConstructorDeclaration.class::cast)
                            .toList()) {

                List<String> constructorParameters =
                        constructor.getParameters()
                                .stream()
                                .map(parameter ->
                                        parameter.getTypeAsString()
                                                + " " + parameter.getNameAsString())
                                .toList();

                List<String> constructorAnnotations =
                        constructor.getAnnotations()
                                .stream()
                                .map(annotation ->
                                        annotation.getNameAsString())
                                .toList();

                String accessModifier =
                        constructor.getAccessSpecifier().asString();

                constructors.add(new JavaConstructorInfo(
                        constructor.getNameAsString(),
                        constructorParameters,
                        constructorAnnotations,
                        accessModifier
                ));
            }

            // Create class information
            JavaClassInfo classInfo = new JavaClassInfo(
                    packageName,
                    type.getNameAsString(),
                    type.getClass().getSimpleName(),
                    annotations,
                    javaFile.toString(),
                    methods,
                    fields,
                    constructors
            );

            classInfos.add(classInfo);
        }

        return classInfos;
    }
    private String determineClassType(TypeDeclaration<?> type) {

        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.isInterface() ? "INTERFACE" : "CLASS";
        }

        if (type instanceof EnumDeclaration) {
            return "ENUM";
        }

        if (type instanceof AnnotationDeclaration) {
            return "ANNOTATION";
        }

        if (type instanceof RecordDeclaration) {
            return "RECORD";
        }

        return "UNKNOWN";
    }
}
