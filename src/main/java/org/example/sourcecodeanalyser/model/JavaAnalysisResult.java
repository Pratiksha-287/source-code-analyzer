
package org.example.sourcecodeanalyser.model;

import java.util.List;

public class JavaAnalysisResult {

    private int totalJavaFiles;
    private int totalClasses;
    private List<JavaClassInfo> classes;

    public JavaAnalysisResult(
            int totalJavaFiles,
            List<JavaClassInfo> classes) {

        this.totalJavaFiles = totalJavaFiles;
        this.classes = classes;

        this.totalClasses = classes.size();
    }

    public int getTotalJavaFiles() {
        return totalJavaFiles;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public List<JavaClassInfo> getClasses() {
        return classes;
    }
}
