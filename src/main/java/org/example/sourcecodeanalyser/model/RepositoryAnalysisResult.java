
package org.example.sourcecodeanalyser.model;

import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.FrontendFileAnalysis;
import org.example.sourcecodeanalyser.model.common.RepositoryInfo;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;
import org.example.sourcecodeanalyser.model.module.ModuleInfo;

import java.util.List;

public class RepositoryAnalysisResult {

    private final String schemaVersion;
    private final RepositoryInfo repositoryInfo;
    private final FileDiscoveryResult fileDiscovery;
    private final JavaAnalysisResult javaAnalysis;
    private final List<FrontendFileAnalysis> frontendAnalysis;
    private final GradleAnalysisResult gradleAnalysis;
    private final List<ModuleInfo> modules;
    private final List<ClientExtensionAnalysis> clientExtensions;
    private final List<ConfigurationFileAnalysis> configurationFiles;

    public RepositoryAnalysisResult(
            RepositoryInfo repositoryInfo,
            FileDiscoveryResult fileDiscovery,
            JavaAnalysisResult javaAnalysis,
            List<FrontendFileAnalysis> frontendAnalysis,
            GradleAnalysisResult gradleAnalysis,
            List<ModuleInfo> modules,
            List<ClientExtensionAnalysis> clientExtensions,
            List<ConfigurationFileAnalysis> configurationFiles) {

        this.schemaVersion = "1.0";
        this.repositoryInfo = repositoryInfo;
        this.fileDiscovery = fileDiscovery;
        this.javaAnalysis = javaAnalysis;
        this.frontendAnalysis = List.copyOf(frontendAnalysis);
        this.gradleAnalysis = gradleAnalysis;
        this.modules = List.copyOf(modules);
        this.clientExtensions = List.copyOf(clientExtensions);
        this.configurationFiles = List.copyOf(configurationFiles);
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    public FileDiscoveryResult getFileDiscovery() {
        return fileDiscovery;
    }

    public JavaAnalysisResult getJavaAnalysis() {
        return javaAnalysis;
    }

    public List<FrontendFileAnalysis> getFrontendAnalysis() {
        return frontendAnalysis;
    }

    public GradleAnalysisResult getGradleAnalysis() {
        return gradleAnalysis;
    }

    public List<ModuleInfo> getModules() {
        return modules;
    }

    public List<ClientExtensionAnalysis> getClientExtensions() {
        return clientExtensions;
    }

    public List<ConfigurationFileAnalysis> getConfigurationFiles() {
        return configurationFiles;
    }
}
