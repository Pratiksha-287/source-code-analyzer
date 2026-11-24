
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class CSSAnalysisResult {

    private FileAnalysisMetadata fileMetadata;
    private List<CSSRuleInfo> rules;
    private List<String> imports;
    private List<String> cssVariables;

    public CSSAnalysisResult(
            FileAnalysisMetadata fileMetadata,
            List<CSSRuleInfo> rules,
            List<String> imports,
            List<String> cssVariables) {

        this.fileMetadata = fileMetadata;
        this.rules = rules;
        this.imports = imports;
        this.cssVariables = cssVariables;
    }

    public FileAnalysisMetadata getFileMetadata() {
        return fileMetadata;
    }

    public List<CSSRuleInfo> getRules() {
        return rules;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getCssVariables() {
        return cssVariables;
    }
}
