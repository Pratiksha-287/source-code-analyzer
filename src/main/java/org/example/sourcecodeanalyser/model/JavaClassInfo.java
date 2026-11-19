
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaClassInfo {

    private String packageName;
    private String className;
    private String classType;
    private List<String> annotations;
    private String filePath;
    private List<JavaMethodInfo> methods;

    public JavaClassInfo(
            String packageName,
            String className,
            String classType,
            List<String> annotations,
            String filePath,
            List<JavaMethodInfo> methods) {

        this.packageName = packageName;
        this.className = className;
        this.classType = classType;
        this.annotations = annotations;
        this.filePath = filePath;
        this.methods = methods;
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
}
