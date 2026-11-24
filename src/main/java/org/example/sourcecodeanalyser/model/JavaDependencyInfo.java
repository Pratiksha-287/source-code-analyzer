package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JavaDependencyInfo {

    private String dependencyName;
    private String dependencyType;
    private String source;
    private String dependencyCategory;
    private String injectionType;
    private String annotationName;
    private String sourceFile;
    private int lineNumber;

    public JavaDependencyInfo(
            String dependencyName,
            String dependencyType,
            String source,
            String dependencyCategory,
            String injectionType,
            String annotationName,
            String sourceFile,
            int lineNumber) {

        this.dependencyName = dependencyName;
        this.dependencyType = dependencyType;
        this.source = source;
        this.dependencyCategory = dependencyCategory;
        this.injectionType = injectionType;
        this.annotationName = annotationName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public JavaDependencyInfo(
            String dependencyName,
            String dependencyType,
            String source,
            String dependencyCategory) {

        this(
                dependencyName,
                dependencyType,
                source,
                dependencyCategory,
                null,
                null,
                null,
                0
        );
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public String getSource() {
        return source;
    }

    public String getDependencyCategory() {
        return dependencyCategory;
    }

    public String getInjectionType() {
        return injectionType;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getLineNumber() {
        return lineNumber;
    }
}