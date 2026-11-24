
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class SCSSAnalysisResult {

    private FileAnalysisMetadata fileMetadata;

    private List<SCSSRuleInfo> rules;
    private List<String> variables;
    private List<String> mixins;
    private List<String> includes;
    private List<String> functions;
    private List<String> imports;
    private List<String> uses;
    private List<String> forwards;
    private List<String> extendsRules;
    private List<String> placeholders;

    public SCSSAnalysisResult(
            FileAnalysisMetadata fileMetadata,
            List<SCSSRuleInfo> rules,
            List<String> variables,
            List<String> mixins,
            List<String> includes,
            List<String> functions,
            List<String> imports,
            List<String> uses,
            List<String> forwards,
            List<String> extendsRules,
            List<String> placeholders) {

        this.fileMetadata = fileMetadata;
        this.rules = rules;
        this.variables = variables;
        this.mixins = mixins;
        this.includes = includes;
        this.functions = functions;
        this.imports = imports;
        this.uses = uses;
        this.forwards = forwards;
        this.extendsRules = extendsRules;
        this.placeholders = placeholders;
    }

    public FileAnalysisMetadata getFileMetadata() {
        return fileMetadata;
    }

    public List<SCSSRuleInfo> getRules() {
        return rules;
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<String> getMixins() {
        return mixins;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getUses() {
        return uses;
    }

    public List<String> getForwards() {
        return forwards;
    }

    public List<String> getExtendsRules() {
        return extendsRules;
    }

    public List<String> getPlaceholders() {
        return placeholders;
    }
}
