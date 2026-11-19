
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaMethodInfo {

    private String methodName;
    private String returnType;
    private List<String> parameters;
    private List<String> annotations;

    public JavaMethodInfo(
            String methodName,
            String returnType,
            List<String> parameters,
            List<String> annotations) {

        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.annotations = annotations;
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
}
