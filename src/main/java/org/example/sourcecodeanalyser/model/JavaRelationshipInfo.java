
package org.example.sourcecodeanalyser.model;

import java.util.List;

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
