
package org.example.sourcecodeanalyser.model;

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

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isWildcard() {
        return isWildcard;
    }
}
