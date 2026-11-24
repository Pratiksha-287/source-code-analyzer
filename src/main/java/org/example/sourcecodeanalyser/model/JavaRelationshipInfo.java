package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JavaRelationshipInfo {

    private String extendedClass;
    private List<String> implementedInterfaces;

    public JavaRelationshipInfo(
            String extendedClass,
            List<String> implementedInterfaces) {

        this.extendedClass = extendedClass;
        this.implementedInterfaces = implementedInterfaces;
    }

    public String getExtendedClass() {
        return extendedClass;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }
}