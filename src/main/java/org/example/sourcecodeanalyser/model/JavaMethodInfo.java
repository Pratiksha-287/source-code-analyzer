
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaMethodInfo {

    private String methodName;
    private String returnType;
    private List<String> parameters;
    private List<String> annotations;
    private List<String> modifiers;
    private int startLine;
    private int endLine;

    public JavaMethodInfo(
            String methodName,
            String returnType,
            List<String> parameters,
            List<String> annotations,
            List<String> modifiers,
            int startLine,
            int endLine) {

        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.annotations = annotations;
        this.modifiers = modifiers;
        this.startLine = startLine;
        this.endLine = endLine;
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

    public List<String> getModifiers() {
        return modifiers;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
}
