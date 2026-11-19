
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaConstructorInfo {

    private String constructorName;
    private List<String> parameters;
    private List<String> annotations;
    private String accessModifier;

    public JavaConstructorInfo(
            String constructorName,
            List<String> parameters,
            List<String> annotations,
            String accessModifier) {

        this.constructorName = constructorName;
        this.parameters = parameters;
        this.annotations = annotations;
        this.accessModifier = accessModifier;
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
}
