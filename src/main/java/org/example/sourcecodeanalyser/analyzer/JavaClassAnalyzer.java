
package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;

import org.example.sourcecodeanalyser.model.JavaAnnotationInfo;
import org.example.sourcecodeanalyser.model.JavaClassInfo;
import org.example.sourcecodeanalyser.model.JavaConstructorInfo;
import org.example.sourcecodeanalyser.model.JavaDependencyInfo;
import org.example.sourcecodeanalyser.model.JavaFieldInfo;
import org.example.sourcecodeanalyser.model.JavaImportInfo;
import org.example.sourcecodeanalyser.model.JavaMethodInfo;
import org.example.sourcecodeanalyser.model.JavaRelationshipInfo;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class JavaClassAnalyzer {

    private final DependencyExtractor dependencyExtractor;

    public JavaClassAnalyzer(
            DependencyExtractor dependencyExtractor) {

        this.dependencyExtractor = dependencyExtractor;
    }

    public List<JavaClassInfo> analyze(
            CompilationUnit compilationUnit,
            Path javaFile,
            Set<String> projectTypes) {

        List<JavaClassInfo> classInfos = new ArrayList<>();

        String packageName = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration ->
                        packageDeclaration.getNameAsString())
                .orElse("");

        // Extract import statements.
        List<JavaImportInfo> imports = new ArrayList<>();

        for (ImportDeclaration importDeclaration :
                compilationUnit.getImports()) {

            imports.add(new JavaImportInfo(
                    importDeclaration.getNameAsString(),
                    importDeclaration.isStatic(),
                    importDeclaration.isAsterisk()
            ));
        }

        // Analyze each top-level type.
        for (TypeDeclaration<?> type : compilationUnit.getTypes()) {

            String classType = determineClassType(type);

            // Extract class-level annotations.
            List<String> annotations = type.getAnnotations()
                    .stream()
                    .map(annotation -> annotation.getNameAsString())
                    .toList();

            // Extract methods.
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

                String methodAccessModifier =
                        method.getAccessSpecifier().asString();

                List<String> methodModifiers = method.getModifiers()
                        .stream()
                        .map(modifier -> modifier.getKeyword().asString())
                        .toList();

                int methodStartLine = method.getRange()
                        .map(range -> range.begin.line)
                        .orElse(0);

                int methodEndLine = method.getRange()
                        .map(range -> range.end.line)
                        .orElse(0);

                methods.add(new JavaMethodInfo(
                        method.getNameAsString(),
                        method.getTypeAsString(),
                        parameters,
                        methodAnnotations,
                        methodAccessModifier,
                        methodModifiers,
                        methodStartLine,
                        methodEndLine
                ));
            }

            // Extract fields.
            List<JavaFieldInfo> fields = new ArrayList<>();

            for (FieldDeclaration field : type.getFields()) {

                String accessModifier =
                        field.getAccessSpecifier().asString();

                List<String> fieldAnnotations =
                        field.getAnnotations()
                                .stream()
                                .map(annotation ->
                                        annotation.getNameAsString())
                                .toList();

                List<String> fieldModifiers = field.getModifiers()
                        .stream()
                        .map(modifier -> modifier.getKeyword().asString())
                        .toList();

                int fieldStartLine = field.getRange()
                        .map(range -> range.begin.line)
                        .orElse(0);

                int fieldEndLine = field.getRange()
                        .map(range -> range.end.line)
                        .orElse(0);

                field.getVariables().forEach(variable -> {

                    fields.add(new JavaFieldInfo(
                            variable.getNameAsString(),
                            variable.getTypeAsString(),
                            accessModifier,
                            fieldAnnotations,
                            fieldModifiers,
                            fieldStartLine,
                            fieldEndLine
                    ));
                });
            }

            // Extract constructors.
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

                List<String> constructorModifiers =
                        constructor.getModifiers()
                                .stream()
                                .map(modifier -> modifier.getKeyword().asString())
                                .toList();

                int constructorStartLine = constructor.getRange()
                        .map(range -> range.begin.line)
                        .orElse(0);

                int constructorEndLine = constructor.getRange()
                        .map(range -> range.end.line)
                        .orElse(0);

                constructors.add(new JavaConstructorInfo(
                        constructor.getNameAsString(),
                        constructorParameters,
                        constructorAnnotations,
                        accessModifier,
                        constructorModifiers,
                        constructorStartLine,
                        constructorEndLine
                ));
            }

            // Extract inheritance and implemented interfaces.
            String extendedClass = null;
            List<String> implementedInterfaces = new ArrayList<>();

            if (type instanceof ClassOrInterfaceDeclaration declaration) {

                extendedClass = declaration.getExtendedTypes()
                        .stream()
                        .map(parent -> parent.getNameAsString())
                        .findFirst()
                        .orElse(null);

                implementedInterfaces = declaration.getImplementedTypes()
                        .stream()
                        .map(interfaceType -> interfaceType.getNameAsString())
                        .toList();
            }

            JavaRelationshipInfo relationships =
                    new JavaRelationshipInfo(
                            extendedClass,
                            implementedInterfaces
                    );

            // Extract class access modifier.
            String classAccessModifier =
                    type.getAccessSpecifier().asString();

            // Extract class modifiers.
            List<String> classModifiers = type.getModifiers()
                    .stream()
                    .map(modifier -> modifier.getKeyword().asString())
                    .toList();

            // Extract source line numbers.
            int startLine = type.getRange()
                    .map(range -> range.begin.line)
                    .orElse(0);

            int endLine = type.getRange()
                    .map(range -> range.end.line)
                    .orElse(0);

            // Extract Java dependencies using DependencyExtractor.
            List<JavaDependencyInfo> dependencies =
                    dependencyExtractor.extract(
                            type,
                            compilationUnit.getImports(),
                            packageName,
                            projectTypes,
                            javaFile
                    );

            // Extract detailed annotation information.
            List<JavaAnnotationInfo> annotationDetails =
                    new ArrayList<>();

            // Class-level annotations.
            type.getAnnotations().forEach(annotation -> {

                annotationDetails.add(new JavaAnnotationInfo(
                        annotation.getNameAsString(),
                        "CLASS",
                        type.getNameAsString()
                ));
            });

            // Method-level annotations.
            for (MethodDeclaration method : type.getMethods()) {

                method.getAnnotations().forEach(annotation -> {

                    annotationDetails.add(new JavaAnnotationInfo(
                            annotation.getNameAsString(),
                            "METHOD",
                            method.getNameAsString()
                    ));
                });
            }

            // Field-level annotations.
            for (FieldDeclaration field : type.getFields()) {

                field.getVariables().forEach(variable -> {

                    field.getAnnotations().forEach(annotation -> {

                        annotationDetails.add(new JavaAnnotationInfo(
                                annotation.getNameAsString(),
                                "FIELD",
                                variable.getNameAsString()
                        ));
                    });
                });
            }

            // Constructor-level annotations.
            for (ConstructorDeclaration constructor :
                    type.getMembers().stream()
                            .filter(ConstructorDeclaration.class::isInstance)
                            .map(ConstructorDeclaration.class::cast)
                            .toList()) {

                constructor.getAnnotations().forEach(annotation -> {

                    annotationDetails.add(new JavaAnnotationInfo(
                            annotation.getNameAsString(),
                            "CONSTRUCTOR",
                            constructor.getNameAsString()
                    ));
                });
            }

            // Create class information.
            JavaClassInfo classInfo = new JavaClassInfo(
                    packageName,
                    type.getNameAsString(),
                    classType,
                    annotations,
                    javaFile.toString(),
                    methods,
                    fields,
                    constructors,
                    relationships,
                    classAccessModifier,
                    classModifiers,
                    imports,
                    dependencies,
                    annotationDetails,
                    startLine,
                    endLine
            );

            classInfos.add(classInfo);
        }

        return classInfos;
    }

    // Identify the type of Java declaration.
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
