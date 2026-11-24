package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
        this.startLine = startLine;
        this.endLine = endLine;
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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getStartLine() {
        return startLine;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getEndLine() {
        return endLine;
    }
}