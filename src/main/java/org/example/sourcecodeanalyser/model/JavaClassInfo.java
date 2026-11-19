
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

    public JavaClassInfo(
            String packageName,
            String className,
            String classType,
            List<String> annotations,
            String filePath,
            List<JavaMethodInfo> methods,
            List<JavaFieldInfo> fields,
            List<JavaConstructorInfo> constructors,
            JavaRelationshipInfo relationships) {

        this.packageName = packageName;
        this.className = className;
        this.classType = classType;
        this.annotations = annotations;
        this.filePath = filePath;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.relationships = relationships;
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
}
