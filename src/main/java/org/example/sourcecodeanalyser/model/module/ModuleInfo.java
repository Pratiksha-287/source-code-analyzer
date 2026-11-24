package org.example.sourcecodeanalyser.model.module;

import java.nio.file.Path;

public class ModuleInfo {

    private final String moduleName;
    private final Path modulePath;
    private final Path buildFile;
    private final Path bndFile;

    public ModuleInfo(
            String moduleName,
            Path modulePath,
            Path buildFile,
            Path bndFile
    ) {
        this.moduleName = moduleName;
        this.modulePath = modulePath;
        this.buildFile = buildFile;
        this.bndFile = bndFile;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getModulePath() {
        return modulePath;
    }

    public Path getBuildFile() {
        return buildFile;
    }

    public Path getBndFile() {
        return bndFile;
    }
}