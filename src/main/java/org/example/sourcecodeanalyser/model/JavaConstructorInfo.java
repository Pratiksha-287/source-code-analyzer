
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaConstructorInfo {

    private String constructorName;
    private List<String> parameters;
    private List<String> annotations;
    private String accessModifier;
    private List<String> modifiers;
    private int startLine;
    private int endLine;

    public JavaConstructorInfo(
            String constructorName,
            List<String> parameters,
            List<String> annotations,
            String accessModifier,
            List<String> modifiers,
            int startLine,
            int endLine) {

        this.constructorName = constructorName;
        this.parameters = parameters;
        this.annotations = annotations;
        this.accessModifier = accessModifier;
        this.modifiers = modifiers;
        this.startLine=startLine;
        this.endLine=endLine;
    }

    public String getConstructorName() {
        return constructorName;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public String getAccessModifier() {
        return accessModifier;
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
