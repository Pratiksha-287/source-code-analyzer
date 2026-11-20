
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaClassInfo {

    private String packageName;
    private String className;
    private String classType;
    private List<String> annotations;
    private String filePath;
    private List<JavaMethodInfo> methods;
    private List<JavaFieldInfo> fields;
    private List<JavaConstructorInfo> constructors;
    private JavaRelationshipInfo relationships;
    private String accessModifier;
    private List<String> modifiers;
    private List<JavaImportInfo> imports;
    private List<JavaDependencyInfo> dependencies;
    private List<JavaAnnotationInfo> annotationDetails;
    private int startLine;
    private int endLine;

    public JavaClassInfo(
            String packageName,
            String className,
            String classType,
            List<String> annotations,
            String filePath,
            List<JavaMethodInfo> methods,
            List<JavaFieldInfo> fields,
            List<JavaConstructorInfo> constructors,
            JavaRelationshipInfo relationships,
            String accessModifier,
            List<String> modifiers,
            List<JavaImportInfo> imports,
            List<JavaDependencyInfo> dependencies,
            List<JavaAnnotationInfo> annotationDetails,
            int startLine,
            int endLine) {

        this.packageName = packageName;
        this.className = className;
        this.classType = classType;
        this.annotations = annotations;
        this.filePath = filePath;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.relationships = relationships;
        this.accessModifier = accessModifier;
        this.modifiers = modifiers;
        this.imports = imports;
        this.dependencies=dependencies;
        this.startLine = startLine;
        this.endLine = endLine;
        this.annotationDetails=annotationDetails;

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

    public String getFilePath() {
        return filePath;
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

    public String getAccessModifier() {
        return accessModifier;
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

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public List<JavaAnnotationInfo> getAnnotationDetails(){ return annotationDetails;}
}
