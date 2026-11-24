
package org.example.sourcecodeanalyser.model;

public class SourceLocation {

    private int startLine;
    private int endLine;

    public SourceLocation(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
}
