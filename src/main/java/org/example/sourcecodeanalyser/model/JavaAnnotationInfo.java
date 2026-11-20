
package org.example.sourcecodeanalyser.model;

public class JavaAnnotationInfo {

    private String annotationName;
    private String targetType;
    private String targetName;

    public JavaAnnotationInfo(
            String annotationName,
            String targetType,
            String targetName) {

        this.annotationName = annotationName;
        this.targetType = targetType;
        this.targetName = targetName;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }
}
