
package org.example.sourcecodeanalyser.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;

import org.example.sourcecodeanalyser.model.JavaDependencyInfo;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Component
public class DependencyExtractor {

    private static final Map<String, String> INJECTION_ANNOTATIONS =
            Map.of(
                    "Autowired", "SPRING_AUTOWIRED",
                    "Inject", "INJECT",
                    "Resource", "RESOURCE_INJECTION",
                    "EJB", "EJB_INJECTION",
                    "Reference", "OSGI_REFERENCE"
            );

    private static final Set<String> JAVA_LANG_TYPES = Set.of(
            "String", "Object", "Integer", "Long", "Double",
            "Float", "Boolean", "Character", "Byte", "Short",
            "Void", "Number", "Class", "Enum", "Record",
            "Throwable", "Exception", "RuntimeException",
            "Error", "StringBuilder", "StringBuffer",
            "Override", "Deprecated", "SuppressWarnings",
            "FunctionalInterface", "SafeVarargs"
    );

    public List<JavaDependencyInfo> extract(
            TypeDeclaration<?> type,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        Map<String, JavaDependencyInfo> dependencies =
                new LinkedHashMap<>();

        // Index class-level field types.
        Map<String, String> fieldTypes = new HashMap<>();

        for (FieldDeclaration field : type.getFields()) {

            String injectionType =
                    determineInjectionType(field.getAnnotations(), "FIELD");

            String annotationName =
                    findInjectionAnnotation(field.getAnnotations());

            for (VariableDeclarator variable : field.getVariables()) {

                String fieldName = variable.getNameAsString();
                String fieldType = variable.getTypeAsString();

                fieldTypes.put(fieldName, fieldType);

                addTypeDependencies(
                        dependencies,
                        fieldType,
                        "FIELD",
                        "Field: " + fieldName,
                        injectionType,
                        annotationName,
                        variable,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );

                detectObjectCreation(
                        variable,
                        dependencies,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }
        }

        // Detect imports, including external and static imports.
        detectImports(
                dependencies,
                imports,
                packageName,
                projectTypes,
                sourceFile
        );

        // Detect inheritance and implemented interfaces.
        if (type instanceof ClassOrInterfaceDeclaration declaration) {

            declaration.getExtendedTypes().forEach(parent ->
                    addTypeDependencies(
                            dependencies,
                            parent.asString(),
                            "EXTENDS",
                            "Class: " + type.getNameAsString(),
                            null,
                            null,
                            parent,
                            imports,
                            packageName,
                            projectTypes,
                            sourceFile
                    )
            );

            declaration.getImplementedTypes().forEach(interfaceType ->
                    addTypeDependencies(
                            dependencies,
                            interfaceType.asString(),
                            "IMPLEMENTS",
                            "Class: " + type.getNameAsString(),
                            null,
                            null,
                            interfaceType,
                            imports,
                            packageName,
                            projectTypes,
                            sourceFile
                    )
            );
        }

        // Detect constructor dependencies and constructor injection.
        for (BodyDeclaration<?> member : type.getMembers()) {

            if (!(member instanceof ConstructorDeclaration constructor)) {
                continue;
            }

            String injectionType =
                    determineInjectionType(
                            constructor.getAnnotations(),
                            "CONSTRUCTOR"
                    );

            String annotationName =
                    findInjectionAnnotation(constructor.getAnnotations());

            Map<String, String> constructorVariables =
                    new HashMap<>(fieldTypes);

            for (Parameter parameter : constructor.getParameters()) {

                String parameterName = parameter.getNameAsString();
                String parameterType = parameter.getTypeAsString();

                constructorVariables.put(parameterName, parameterType);

                addTypeDependencies(
                        dependencies,
                        parameterType,
                        "CONSTRUCTOR_PARAMETER",
                        "Constructor: " + constructor.getNameAsString(),
                        injectionType,
                        annotationName,
                        parameter,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }

            detectLocalVariables(
                    constructor,
                    constructorVariables,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile,
                    "Constructor: " + constructor.getNameAsString()
            );

            detectObjectCreation(
                    constructor,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            detectMethodCalls(
                    constructor,
                    constructorVariables,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            detectMethodReferences(
                    constructor,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        }

        // Detect method dependencies and method injection.
        for (MethodDeclaration method : type.getMethods()) {

            String injectionType =
                    determineInjectionType(
                            method.getAnnotations(),
                            "METHOD"
                    );

            String annotationName =
                    findInjectionAnnotation(method.getAnnotations());

            boolean setterInjection =
                    injectionType != null
                            && method.getNameAsString().startsWith("set");

            Map<String, String> methodVariables =
                    new HashMap<>(fieldTypes);

            for (Parameter parameter : method.getParameters()) {

                String parameterName = parameter.getNameAsString();
                String parameterType = parameter.getTypeAsString();

                methodVariables.put(parameterName, parameterType);

                addTypeDependencies(
                        dependencies,
                        parameterType,
                        "METHOD_PARAMETER",
                        "Method: " + method.getNameAsString(),
                        setterInjection ? "SETTER_INJECTION" : injectionType,
                        annotationName,
                        parameter,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }

            // Detect method return type.
            addTypeDependencies(
                    dependencies,
                    method.getTypeAsString(),
                    "METHOD_RETURN_TYPE",
                    "Method: " + method.getNameAsString(),
                    null,
                    null,
                    method,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            detectLocalVariables(
                    method,
                    methodVariables,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile,
                    "Method: " + method.getNameAsString()
            );

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
                    methodVariables,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );

            detectMethodReferences(
                    method,
                    dependencies,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        }

        // Detect annotation dependencies.
        type.findAll(AnnotationExpr.class).forEach(annotation -> {

            String annotationName = annotation.getNameAsString();

            addDependency(
                    dependencies,
                    annotationName,
                    "ANNOTATION",
                    "Annotation: @" + annotationName,
                    null,
                    "@" + annotationName,
                    annotation,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });

        // Identify the current class's fully qualified name.
        String currentClassName = packageName.isBlank()
                ? type.getNameAsString()
                : packageName + "." + type.getNameAsString();

        // Remove self-dependencies.
        dependencies.values().removeIf(dependency ->
                dependency.getDependencyName().equals(currentClassName)
        );

        return new ArrayList<>(dependencies.values());
    }

    private void detectImports(
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        for (ImportDeclaration declaration : imports) {

            String importedName = declaration.getNameAsString();

            if (declaration.isStatic()) {

                String ownerName = importedName;

                if (!declaration.isAsterisk()) {

                    int lastDot = importedName.lastIndexOf('.');

                    if (lastDot > 0) {
                        ownerName = importedName.substring(0, lastDot);
                    }
                }

                addDependency(
                        dependencies,
                        ownerName,
                        "STATIC_IMPORT",
                        "Static import: " + importedName,
                        null,
                        null,
                        declaration,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );

            } else if (declaration.isAsterisk()) {

                addDependency(
                        dependencies,
                        importedName + ".*",
                        "WILDCARD_IMPORT",
                        "Wildcard import: " + importedName,
                        null,
                        null,
                        declaration,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );

            } else {

                addDependency(
                        dependencies,
                        importedName,
                        "IMPORT",
                        "Import: " + importedName,
                        null,
                        null,
                        declaration,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }
        }
    }

    private void detectLocalVariables(
            Node node,
            Map<String, String> variableTypes,
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile,
            String source) {

        node.findAll(VariableDeclarator.class).forEach(variable -> {

            String variableName = variable.getNameAsString();
            String variableType = variable.getTypeAsString();

            variableTypes.put(variableName, variableType);

            addTypeDependencies(
                    dependencies,
                    variableType,
                    "LOCAL_VARIABLE",
                    source,
                    null,
                    null,
                    variable,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });

        // Catch parameters are also local variables.
        node.findAll(CatchClause.class).forEach(catchClause -> {

            Parameter parameter = catchClause.getParameter();

            variableTypes.put(
                    parameter.getNameAsString(),
                    parameter.getTypeAsString()
            );

            addTypeDependencies(
                    dependencies,
                    parameter.getTypeAsString(),
                    "CATCH_PARAMETER",
                    source,
                    null,
                    null,
                    parameter,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });

        // Lambda parameters with explicitly declared types.
        node.findAll(LambdaExpr.class).forEach(lambda -> {

            lambda.getParameters().forEach(parameter -> {

                if (!parameter.getType().isUnknownType()) {

                    variableTypes.put(
                            parameter.getNameAsString(),
                            parameter.getTypeAsString()
                    );
                }
            });
        });
    }

    private void detectObjectCreation(
            Node node,
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        node.findAll(ObjectCreationExpr.class).forEach(expression -> {

            addTypeDependencies(
                    dependencies,
                    expression.getTypeAsString(),
                    "OBJECT_CREATION",
                    "Object creation: new " + expression.getTypeAsString(),
                    null,
                    null,
                    expression,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });
    }

    private void detectMethodCalls(
            Node node,
            Map<String, String> variableTypes,
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        node.findAll(MethodCallExpr.class).forEach(methodCall -> {

            if (methodCall.getScope().isEmpty()) {
                return;
            }

            Expression scope = methodCall.getScope().get();

            String scopeName;

            if (scope instanceof NameExpr nameExpr) {

                String name = nameExpr.getNameAsString();

                if (variableTypes.containsKey(name)) {

                    scopeName = variableTypes.get(name);

                } else {

                    // Only treat an unresolved simple name as a class
                    // when it matches an import or project type.
                    scopeName = resolveTypeName(
                            name,
                            imports,
                            packageName,
                            projectTypes
                    );

                    if (scopeName.equals(name)
                            && !projectTypes.contains(name)
                            && !hasMatchingImport(name, imports)) {
                        return;
                    }
                }

            } else if (scope instanceof ThisExpr) {

                return;

            } else if (scope instanceof SuperExpr) {

                return;

            } else {

                // Avoid recording complete expressions such as
                // service.getClient() as if they were class names.
                return;
            }

            addDependency(
                    dependencies,
                    scopeName,
                    "METHOD_CALL",
                    "Method call: " + methodCall.getNameAsString(),
                    null,
                    null,
                    methodCall,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });
    }

    private void detectMethodReferences(
            Node node,
            Map<String, JavaDependencyInfo> dependencies,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        node.findAll(MethodReferenceExpr.class).forEach(reference -> {

            String scopeName = reference.getScope().toString();

            addDependency(
                    dependencies,
                    scopeName,
                    "METHOD_REFERENCE",
                    "Method reference: " + reference.getIdentifier(),
                    null,
                    null,
                    reference,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        });
    }

    private void addTypeDependencies(
            Map<String, JavaDependencyInfo> dependencies,
            String typeName,
            String dependencyType,
            String source,
            String injectionType,
            String annotationName,
            Node node,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        if (typeName == null || typeName.isBlank()) {
            return;
        }

        try {

            com.github.javaparser.ast.type.Type parsedType =
                    StaticJavaParser.parseType(typeName);

            Set<String> referencedTypes = new LinkedHashSet<>();

            parsedType.findAll(
                    com.github.javaparser.ast.type.ClassOrInterfaceType.class
            ).forEach(classType ->
                    referencedTypes.add(classType.getNameWithScope())
            );

            if (parsedType.isClassOrInterfaceType()) {
                referencedTypes.add(
                        parsedType.asClassOrInterfaceType().getNameWithScope()
                );
            }

            for (String referencedType : referencedTypes) {

                addDependency(
                        dependencies,
                        referencedType,
                        dependencyType,
                        source,
                        injectionType,
                        annotationName,
                        node,
                        imports,
                        packageName,
                        projectTypes,
                        sourceFile
                );
            }

        } catch (Exception ignored) {

            // Fall back to the original type name if parsing fails.
            addDependency(
                    dependencies,
                    typeName,
                    dependencyType,
                    source,
                    injectionType,
                    annotationName,
                    node,
                    imports,
                    packageName,
                    projectTypes,
                    sourceFile
            );
        }
    }

    private void addDependency(
            Map<String, JavaDependencyInfo> dependencies,
            String dependencyName,
            String dependencyType,
            String source,
            String injectionType,
            String annotationName,
            Node node,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes,
            Path sourceFile) {

        if (dependencyName == null || dependencyName.isBlank()) {
            return;
        }

        String normalizedName = dependencyName.trim();

        if (normalizedName.endsWith(".*")) {
            normalizedName = normalizedName.substring(
                    0,
                    normalizedName.length() - 2
            );

        } else {

            if (isBasicJavaType(normalizedName)) {
                return;
            }

            normalizedName = resolveTypeName(
                    normalizedName,
                    imports,
                    packageName,
                    projectTypes
            );
        }

        if (normalizedName.isBlank()) {
            return;
        }

        String category = determineDependencyCategory(
                normalizedName,
                imports,
                packageName,
                projectTypes
        );

        int lineNumber = node.getRange()
                .map(range -> range.begin.line)
                .orElse(0);

        String key = normalizedName + "|"
                + dependencyType + "|"
                + source + "|"
                + lineNumber;

        dependencies.putIfAbsent(
                key,
                new JavaDependencyInfo(
                        normalizedName,
                        dependencyType,
                        source,
                        category,
                        injectionType,
                        annotationName,
                        sourceFile.toString().replace('\\', '/'),
                        lineNumber
                )
        );
    }

    private String resolveTypeName(
            String typeName,
            List<ImportDeclaration> imports,
            String packageName,
            Set<String> projectTypes) {

        String normalized = typeName.trim();

        if (normalized.contains("<")) {
            normalized = normalized.substring(
                    0,
                    normalized.indexOf('<')
            );
        }

        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }

        // Already fully qualified.
        if (normalized.contains(".")) {
            return normalized;
        }

        // Resolve explicit imports, including static imports.
        for (ImportDeclaration declaration : imports) {

            String imported = declaration.getNameAsString();

            if (declaration.isStatic() && !declaration.isAsterisk()) {

                int lastDot = imported.lastIndexOf('.');

                if (lastDot > 0) {
                    imported = imported.substring(0, lastDot);
                }
            }

            if (!declaration.isAsterisk()) {

                String simpleName =
                        imported.substring(imported.lastIndexOf('.') + 1);

                if (simpleName.equals(normalized)) {
                    return imported;
                }

            } else if (!declaration.isStatic()) {

                String candidate = imported + "." + normalized;

                if (projectTypes.contains(candidate)) {
                    return candidate;
                }
            }
        }

        // Resolve types from the same package.
        String samePackage = packageName.isBlank()
                ? normalized
                : packageName + "." + normalized;

        if (projectTypes.contains(samePackage)) {
            return samePackage;
        }

        // Resolve common java.lang types without explicit imports.
        if (JAVA_LANG_TYPES.contains(normalized)) {
            return "java.lang." + normalized;
        }

        return normalized;
    }

    private boolean hasMatchingImport(
            String typeName,
            List<ImportDeclaration> imports) {

        for (ImportDeclaration declaration : imports) {

            String imported = declaration.getNameAsString();

            if (declaration.isStatic() && !declaration.isAsterisk()) {

                int lastDot = imported.lastIndexOf('.');

                if (lastDot > 0) {
                    imported = imported.substring(0, lastDot);
                }
            }

            if (!declaration.isAsterisk()
                    && imported.endsWith("." + typeName)) {
                return true;
            }
        }

        return false;
    }

    private String determineInjectionType(
            List<AnnotationExpr> annotations,
            String target) {

        for (AnnotationExpr annotation : annotations) {

            String name = getSimpleName(annotation.getNameAsString());

            if (!INJECTION_ANNOTATIONS.containsKey(name)) {
                continue;
            }

            String annotationType = INJECTION_ANNOTATIONS.get(name);

            if ("OSGI_REFERENCE".equals(annotationType)
                    || "RESOURCE_INJECTION".equals(annotationType)
                    || "EJB_INJECTION".equals(annotationType)) {
                return annotationType;
            }

            if ("CONSTRUCTOR".equals(target)) {
                return "CONSTRUCTOR_INJECTION";
            }

            if ("METHOD".equals(target)) {
                return "METHOD_INJECTION";
            }

            return "FIELD_INJECTION";
        }

        return null;
    }

    private String findInjectionAnnotation(
            List<AnnotationExpr> annotations) {

        for (AnnotationExpr annotation : annotations) {

            String name = getSimpleName(annotation.getNameAsString());

            if (INJECTION_ANNOTATIONS.containsKey(name)) {
                return "@" + name;
            }
        }

        return null;
    }

    private String getSimpleName(String name) {

        return name.substring(name.lastIndexOf('.') + 1);
    }

    private boolean isBasicJavaType(String typeName) {

        if (typeName == null || typeName.isBlank()) {
            return true;
        }

        String normalized = typeName.trim();

        if (normalized.contains("<")) {
            normalized = normalized.substring(
                    0,
                    normalized.indexOf('<')
            );
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

        if (projectTypes.contains(normalized)) {
            return "INTERNAL_PROJECT";
        }

        if (normalized.startsWith("java.")) {
            return "STANDARD_LIBRARY";
        }

        String resolvedName = resolveTypeName(
                normalized,
                imports,
                packageName,
                projectTypes
        );

        if (projectTypes.contains(resolvedName)) {
            return "INTERNAL_PROJECT";
        }

        if (resolvedName.startsWith("java.")) {
            return "STANDARD_LIBRARY";
        }

        if (isExternalFramework(resolvedName)) {
            return "EXTERNAL_FRAMEWORK";
        }

        // A fully qualified imported class outside the scanned
        // repository is an external dependency, even if its
        // framework is not in the recognized package list.
        if (resolvedName.contains(".")) {
            return "EXTERNAL_DEPENDENCY";
        }

        return "UNKNOWN";
    }

    private boolean isExternalFramework(String name) {

        return name.startsWith("javax.")
                || name.startsWith("jakarta.")
                || name.startsWith("org.springframework.")
                || name.startsWith("org.osgi.")
                || name.startsWith("com.liferay.")
                || name.startsWith("org.apache.")
                || name.startsWith("com.fasterxml.")
                || name.startsWith("org.slf4j.")
                || name.startsWith("org.hibernate.")
                || name.startsWith("org.junit.")
                || name.startsWith("org.mockito.");
    }
}
