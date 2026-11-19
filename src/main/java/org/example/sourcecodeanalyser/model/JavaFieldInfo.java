
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaFieldInfo {

    private String fieldName;
    private String fieldType;
    private String accessModifier;
    private List<String> annotations;

    public JavaFieldInfo(
            String fieldName,
            String fieldType,
            String accessModifier,
            List<String> annotations) {

        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.accessModifier = accessModifier;
        this.annotations = annotations;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getAccessModifier() {
        return accessModifier;
    }

    public List<String> getAnnotations() {
        return annotations;
    }
}
