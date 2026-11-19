
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaMethodInfo {

    private String methodName;
    private String returnType;
    private List<String> parameters;
    private List<String> annotations;
    private String accessModifier;
    private List<String> modifiers;

    public JavaMethodInfo(
            String methodName,
            String returnType,
            List<String> parameters,
            List<String> annotations,
            String accessModifier,
            List<String> modifiers) {

        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.annotations = annotations;
        this.accessModifier = accessModifier;
        this.modifiers = modifiers;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
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
