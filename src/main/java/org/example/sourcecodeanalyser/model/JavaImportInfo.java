package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JavaImportInfo {

    private String importName;
    private boolean isStatic;
    private boolean isWildcard;

    public JavaImportInfo(
            String importName,
            boolean isStatic,
            boolean isWildcard) {

        this.importName = importName;
        this.isStatic = isStatic;
        this.isWildcard = isWildcard;
    }

    public String getImportName() {
        return importName;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isStatic() {
        return isStatic;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isWildcard() {
        return isWildcard;
    }
}