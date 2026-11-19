
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaConstructorInfo {

    private String constructorName;
    private List<String> parameters;
    private List<String> annotations;
    private String accessModifier;
    private List<String> modifiers;

    public JavaConstructorInfo(
            String constructorName,
            List<String> parameters,
            List<String> annotations,
            String accessModifier,
            List<String> modifiers) {

        this.constructorName = constructorName;
        this.parameters = parameters;
        this.annotations = annotations;
        this.accessModifier = accessModifier;
        this.modifiers = modifiers;
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
}
