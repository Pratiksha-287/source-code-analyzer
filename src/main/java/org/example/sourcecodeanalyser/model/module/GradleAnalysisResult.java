
package org.example.sourcecodeanalyser.model.module;

import java.util.List;

public class GradleAnalysisResult {

    private final List<GradleInfo> workspaceGradleFiles;
    private final List<GradleInfo> moduleGradleFiles;

    public GradleAnalysisResult(
            List<GradleInfo> workspaceGradleFiles,
            List<GradleInfo> moduleGradleFiles) {

        this.workspaceGradleFiles = workspaceGradleFiles;
        this.moduleGradleFiles = moduleGradleFiles;
    }

    public List<GradleInfo> getWorkspaceGradleFiles() {
        return workspaceGradleFiles;
    }

    public List<GradleInfo> getModuleGradleFiles() {
        return moduleGradleFiles;
    }
}
