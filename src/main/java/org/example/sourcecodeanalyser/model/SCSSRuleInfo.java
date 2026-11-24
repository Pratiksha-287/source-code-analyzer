package org.example.sourcecodeanalyser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SCSSRuleInfo {

    private String selector;
    private List<String> declarations;
    private int startLine;
    private int endLine;

    public SCSSRuleInfo(
            String selector,
            List<String> declarations,
            int startLine,
            int endLine) {

        this.selector = selector;
        this.declarations = declarations;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getSelector() {
        return selector;
    }

    public List<String> getDeclarations() {
        return declarations;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getStartLine() {
        return startLine;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getEndLine() {
        return endLine;
    }
}