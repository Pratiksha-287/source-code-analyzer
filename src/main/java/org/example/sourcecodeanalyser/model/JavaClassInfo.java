
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaClassInfo {

    private String packageName;
    private String className;
    private String classType;
    private List<String> annotations;
    private FileAnalysisMetadata fileMetadata;
    private List<JavaMethodInfo> methods;
    private List<JavaFieldInfo> fields;
    private List<JavaConstructorInfo> constructors;
    private JavaRelationshipInfo relationships;
    private List<String> modifiers;
    private List<JavaImportInfo> imports;
    private List<JavaDependencyInfo> dependencies;
    private List<JavaAnnotationInfo> annotationDetails;
    private SourceLocation sourceLocation;

    public JavaClassInfo(
            String packageName,
            String className,
            String classType,
            List<String> annotations,
            FileAnalysisMetadata fileMetadata,
            List<JavaMethodInfo> methods,
            List<JavaFieldInfo> fields,
            List<JavaConstructorInfo> constructors,
            JavaRelationshipInfo relationships,
            List<String> modifiers,
            List<JavaImportInfo> imports,
            List<JavaDependencyInfo> dependencies,
            List<JavaAnnotationInfo> annotationDetails,
            SourceLocation sourceLocation) {

        this.packageName = packageName;
        this.className = className;
        this.classType = classType;
        this.annotations = annotations;
        this.fileMetadata = fileMetadata;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.relationships = relationships;
        this.modifiers = modifiers;
        this.imports = imports;
        this.dependencies = dependencies;
        this.annotationDetails = annotationDetails;
        this.sourceLocation = sourceLocation;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getClassType() {
        return classType;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public FileAnalysisMetadata getFileMetadata() {
        return fileMetadata;
    }

    public List<JavaMethodInfo> getMethods() {
        return methods;
    }

    public List<JavaFieldInfo> getFields() {
        return fields;
    }

    public List<JavaConstructorInfo> getConstructors() {
        return constructors;
    }

    public JavaRelationshipInfo getRelationships() {
        return relationships;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public List<JavaImportInfo> getImports() {
        return imports;
    }

    public List<JavaDependencyInfo> getDependencies() {
        return dependencies;
    }

    public List<JavaAnnotationInfo> getAnnotationDetails() {
        return annotationDetails;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
