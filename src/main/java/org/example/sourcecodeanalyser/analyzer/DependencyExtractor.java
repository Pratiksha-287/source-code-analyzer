
package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import org.example.sourcecodeanalyser.model.JavaDependencyInfo;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Component
public class DependencyExtractor {

    private static final Map<String, String> INJECTION_ANNOTATIONS =
            Map.of(
                    "Autowired", "FIELD_INJECTION",
                    "Inject", "FIELD_INJECTION",
                    "Resource", "RESOURCE_INJECTION",
                    "EJB", "EJB_INJECTION",
                    "Reference", "OSGI_REFERENCE"
            );

    public List<JavaDependencyInfo> extract(
            TypeDeclaration<?> type,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        Map<String, JavaDependencyInfo> dependencies =
                new LinkedHashMap<>();

        Map<String, String> variableTypes = new HashMap<>();


        // Detect dependencies from explicit imports.
        for (ImportDeclaration declaration : imports) {

            String importedName = declaration.getNameAsString();

            if (declaration.isAsterisk()) {
                continue;
            }

            // For static imports, identify the owning class.
            if (declaration.isStatic()) {

                int lastDot = importedName.lastIndexOf('.');

                if (lastDot == -1) {
                    continue;
                }

                importedName = importedName.substring(0, lastDot);
            }

            if (projectTypes.contains(importedName)) {

                addDependency(
                        dependencies,
                        importedName,
                        declaration.isStatic()
                                ? "STATIC_IMPORT"
                                : "IMPORT",
                        "Import: " + declaration.getNameAsString(),
                        null,
                        null,
                        declaration.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }
        }

        // Detect inheritance and implemented interfaces.
        if (type instanceof ClassOrInterfaceDeclaration declaration) {

            declaration.getExtendedTypes().forEach(parent -> {

                addDependency(
                        dependencies,
                        parent.getNameAsString(),
                        "EXTENDS",
                        "Class: " + type.getNameAsString(),
                        null,
                        null,
                        parent.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            });

            declaration.getImplementedTypes().forEach(interfaceType -> {

                addDependency(
                        dependencies,
                        interfaceType.getNameAsString(),
                        "IMPLEMENTS",
                        "Class: " + type.getNameAsString(),
                        null,
                        null,
                        interfaceType.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            });
        }


        // Index class fields and detect field injection.
        for (FieldDeclaration field : type.getFields()) {

            String injectionType = determineInjectionType(
                    field.getAnnotations()
            );

            String annotationName = findInjectionAnnotation(
                    field.getAnnotations()
            );

            for (VariableDeclarator variable : field.getVariables()) {

                String fieldType = variable.getTypeAsString();

                variableTypes.put(
                        variable.getNameAsString(),
                        fieldType
                );

                addDependency(
                        dependencies,
                        fieldType,
                        injectionType == null ? "FIELD" : injectionType,
                        "Field: " + variable.getNameAsString(),
                        injectionType,
                        annotationName,
                        field.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }
        }

        // Detect constructor parameters and constructor injection.
        List<ConstructorDeclaration> constructors =
                type.getMembers().stream()
                        .filter(ConstructorDeclaration.class::isInstance)
                        .map(ConstructorDeclaration.class::cast)
                        .toList();

        for (ConstructorDeclaration constructor : constructors) {

            String injectionType = determineInjectionType(
                    constructor.getAnnotations()
            );

            String annotationName = findInjectionAnnotation(
                    constructor.getAnnotations()
            );

            for (Parameter parameter : constructor.getParameters()) {

                variableTypes.put(
                        parameter.getNameAsString(),
                        parameter.getTypeAsString()
                );

                addDependency(
                        dependencies,
                        parameter.getTypeAsString(),
                        "CONSTRUCTOR_PARAMETER",
                        "Constructor: " + constructor.getNameAsString(),
                        injectionType == null
                                ? null
                                : "CONSTRUCTOR_INJECTION",
                        annotationName,
                        parameter.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }

            // Detect object creation inside constructors.
            detectObjectCreation(
                    constructor,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        }

        // Detect method parameters, setter injection and method bodies.
        for (MethodDeclaration method : type.getMethods()) {

            String methodInjection = determineInjectionType(
                    method.getAnnotations()
            );

            String annotationName = findInjectionAnnotation(
                    method.getAnnotations()
            );

            boolean setterInjection =
                    methodInjection != null
                            && method.getNameAsString().startsWith("set");

            for (Parameter parameter : method.getParameters()) {

                variableTypes.put(
                        parameter.getNameAsString(),
                        parameter.getTypeAsString()
                );

                addDependency(
                        dependencies,
                        parameter.getTypeAsString(),
                        "METHOD_PARAMETER",
                        "Method: " + method.getNameAsString(),
                        setterInjection ? "SETTER_INJECTION"
                                : methodInjection == null
                                ? null : "METHOD_INJECTION",
                        annotationName,
                        parameter.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }

            // Detect method return types.
            addDependency(
                    dependencies,
                    method.getTypeAsString(),
                    "METHOD_RETURN_TYPE",
                    "Method: " + method.getNameAsString(),
                    null,
                    null,
                    method.getRange()
                            .map(range -> range.begin.line)
                            .orElse(0),
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            // Detect local variable declarations.
            method.findAll(VariableDeclarator.class).forEach(variable -> {

                String variableType = variable.getTypeAsString();

                variableTypes.put(
                        variable.getNameAsString(),
                        variableType
                );

                addDependency(
                        dependencies,
                        variableType,
                        "LOCAL_VARIABLE",
                        "Method: " + method.getNameAsString(),
                        null,
                        null,
                        variable.getRange()
                                .map(range -> range.begin.line)
                                .orElse(0),
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            });

            // Detect object creation and method calls.
            detectObjectCreation(
                    method,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            detectMethodCalls(
                    method,
                    dependencies,
                    variableTypes,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            // Detect method references such as UserService::save.
            method.findAll(MethodReferenceExpr.class)
                    .forEach(reference -> {

                        String referenceName =
                                reference.getIdentifier();

                        String scopeName =
                                reference.getScope().toString();

                        addDependency(
                                dependencies,
                                scopeName,
                                "METHOD_REFERENCE",
                                "Method reference: " + referenceName,
                                null,
                                null,
                                reference.getRange()
                                        .map(range -> range.begin.line)
                                        .orElse(0),
                                imports,
                                packageName,
                                projectTypes,
                                sourceFile
                        );
                    });
        }

        // Detect annotation-based class relationships.
        type.findAll(AnnotationExpr.class).forEach(annotation -> {

            String annotationName =
                    annotation.getNameAsString();

            addDependency(
                    dependencies,
                    annotationName,
                    "ANNOTATION",
                    "Annotation on " + type.getNameAsString(),
                    null,
                    annotationName,
                    annotation.getRange()
                            .map(range -> range.begin.line)
                            .orElse(0),
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });

        return new ArrayList<>(dependencies.values());
    }

    private void detectObjectCreation(
            com.github.javaparser.ast.Node node,
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        node.findAll(ObjectCreationExpr.class)
                .forEach(expression -> {

                    addDependency(
                            dependencies,
                            expression.getTypeAsString(),
                            "OBJECT_CREATION",
                            "new " + expression.getTypeAsString(),
                            null,
                            null,
                            expression.getRange()
                                    .map(range -> range.begin.line)
                                    .orElse(0),
                            imports,
                            packageName,
                            projectTypes,
                            sourceFile
                    );
                });
    }

    private void detectMethodCalls(
            com.github.javaparser.ast.Node node,
            Map<String, JavaDependencyInfo> dependencies,
            Map<String, String> variableTypes,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        node.findAll(MethodCallExpr.class)
                .forEach(methodCall -> {

                    if (methodCall.getScope().isEmpty()) {
                        return;
                    }

                    String scopeName =
                            methodCall.getScope().get().toString();

                    String dependencyName =
                            variableTypes.getOrDefault(
                                    scopeName,
                                    scopeName
                            );

                    addDependency(
                            dependencies,
                            dependencyName,
                            "METHOD_CALL",
                            "Method call: "
                                    + methodCall.getNameAsString(),
                            null,
                            null,
                            methodCall.getRange()
                                    .map(range -> range.begin.line)
                                    .orElse(0),
                            imports,
                            packageName,
                            projectTypes,
                            sourceFile
                    );
                });
    }

    private void addDependency(
            Map<String, JavaDependencyInfo> dependencies,
            String dependencyName,
            String dependencyType,
            String source,
            String injectionType,
            String annotationName,
            int lineNumber,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        if (dependencyName == null || dependencyName.isBlank()) {
            return;
        }

        if (isBasicJavaType(dependencyName)) {
            return;
        }

        String category = determineDependencyCategory(
                dependencyName,
                imports,
                packageName,
                projectTypes
        );

        String key = dependencyName + "|"
                + dependencyType + "|"
                + source + "|"
                + lineNumber;

        dependencies.putIfAbsent(
                key,
                new JavaDependencyInfo(
                        dependencyName,
                        dependencyType,
                        source,
                        category,
                        injectionType,
                        annotationName,
                        sourceFile.toString(),
                        lineNumber
                )
        );
    }

    private String determineInjectionType(
            List<com.github.javaparser.ast.expr.AnnotationExpr> annotations) {

        for (var annotation : annotations) {

            String name = annotation.getNameAsString();

            String simpleName = name.substring(
                    name.lastIndexOf('.') + 1
            );

            if (INJECTION_ANNOTATIONS.containsKey(simpleName)) {
                return INJECTION_ANNOTATIONS.get(simpleName);
            }
        }

        return null;
    }

    private String findInjectionAnnotation(
            List<com.github.javaparser.ast.expr.AnnotationExpr> annotations) {

        for (var annotation : annotations) {

            String name = annotation.getNameAsString();

            String simpleName = name.substring(
                    name.lastIndexOf('.') + 1
            );

            if (INJECTION_ANNOTATIONS.containsKey(simpleName)) {
                return "@" + simpleName;
            }
        }

        return null;
    }

    private boolean isBasicJavaType(String typeName) {

        if (typeName == null || typeName.isBlank()) {
            return true;
        }

        String normalized = typeName.trim();

        int genericStart = normalized.indexOf('<');

        if (genericStart != -1) {
            normalized = normalized.substring(0, genericStart).trim();
        }

        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 2
            );
        }

        if (normalized.endsWith("...")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 3
            );
        }

        if (normalized.startsWith("java.lang.")) {
            normalized = normalized.substring("java.lang.".length());
        }

        return Set.of(
                "byte", "short", "int", "long",
                "float", "double", "boolean", "char",
                "void", "String", "Object",
                "Integer", "Long", "Double",
                "Float", "Boolean", "Character",
                "Byte", "Short", "Void", "Number"
        ).contains(normalized);
    }

    private String determineDependencyCategory(
            String dependencyName,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes) {

        String normalized = dependencyName.trim();

        int genericStart = normalized.indexOf('<');

        if (genericStart != -1) {
            normalized = normalized.substring(0, genericStart).trim();
        }

        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 2
            );
        }

        if (projectTypes.contains(normalized)) {
            return "INTERNAL_PROJECT";
        }

        for (ImportDeclaration declaration : imports) {

            if (declaration.isStatic()) {
                continue;
            }

            String imported = declaration.getNameAsString();

            if (!declaration.isAsterisk()) {

                String simpleName = imported.substring(
                        imported.lastIndexOf('.') + 1
                );

                if (simpleName.equals(normalized)
                        || imported.equals(normalized)) {

                    if (projectTypes.contains(imported)) {
                        return "INTERNAL_PROJECT";
                    }

                    return classifyPackage(imported);
                }
            } else {

                String candidate = imported + "." + normalized;

                if (projectTypes.contains(candidate)) {
                    return "INTERNAL_PROJECT";
                }

                if (imported.startsWith("java.")) {
                    return "STANDARD_LIBRARY";
                }

                if (isExternalFramework(imported)) {
                    return "EXTERNAL_FRAMEWORK";
                }
            }
        }

        if (!packageName.isBlank()
                && projectTypes.contains(packageName + "." + normalized)) {
            return "INTERNAL_PROJECT";
        }

        if (normalized.startsWith("java.")) {
            return "STANDARD_LIBRARY";
        }

        return classifyPackage(normalized);
    }

    private String classifyPackage(String name) {

        if (name.startsWith("java.")) {
            return "STANDARD_LIBRARY";
        }

        if (isExternalFramework(name)) {
            return "EXTERNAL_FRAMEWORK";
        }

        return "UNKNOWN";
    }

    private boolean isExternalFramework(String name) {

        return name.startsWith("javax.")
                || name.startsWith("jakarta.")
                || name.startsWith("org.springframework.")
                || name.startsWith("com.liferay.")
                || name.startsWith("org.apache.")
                || name.startsWith("com.fasterxml.");
    }
}
