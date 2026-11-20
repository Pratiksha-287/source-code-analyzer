
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaFieldInfo {

    private String fieldName;
    private String fieldType;
    private String accessModifier;
    private List<String> annotations;
    private List<String> modifiers;
    private int startLine;
    private int endLine;

    public JavaFieldInfo(
            String fieldName,
            String fieldType,
            String accessModifier,
            List<String> annotations,
            List<String> modifiers,
            int startLine,
            int endLine) {

        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.accessModifier = accessModifier;
        this.annotations = annotations;
        this.modifiers = modifiers;
        this.startLine=startLine;
        this.endLine=endLine;
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

    public List<String> getModifiers() {
        return modifiers;
    }

    public int getStartLine(){
        return startLine;
    }

    public int getEndLine(){
        return endLine;
    }
}
