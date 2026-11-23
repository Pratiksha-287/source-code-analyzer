package org.example.sourcecodeanalyser.model;

import java.util.List;

public class CSSAnalysisResult {

    private String filePath;
    private int totalRules;
    private List<CSSRuleInfo> rules;
    private List<String> imports;
    private List<String> cssVariables;

    public CSSAnalysisResult(
            String filePath,
            int totalRules,
            List<CSSRuleInfo> rules,
            List<String> imports,
            List<String> cssVariables) {

        this.filePath = filePath;
        this.totalRules = totalRules;
        this.rules = rules;
        this.imports = imports;
        this.cssVariables = cssVariables;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getTotalRules() {
        return totalRules;
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