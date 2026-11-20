
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.analyzer.GradleAnalyzer;
import org.example.sourcecodeanalyser.analyzer.clientextension.ClientExtensionAnalyzer;
import org.example.sourcecodeanalyser.discovery.ClientExtensionDiscovery;
import org.example.sourcecodeanalyser.discovery.ModuleDiscovery;
import org.example.sourcecodeanalyser.discovery.RepositoryDiscovery;
import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.example.sourcecodeanalyser.model.common.RepositoryInfo;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;
import org.example.sourcecodeanalyser.model.module.GradleInfo;
import org.example.sourcecodeanalyser.model.module.ModuleInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepositoryAnalysisService {

    private final RepositoryScanService repositoryScanService;
    private final JavaAnalysisService javaAnalysisService;
    private final RepositoryDiscovery repositoryDiscovery;
    private final ClientExtensionDiscovery clientExtensionDiscovery;
    private final ClientExtensionAnalyzer clientExtensionAnalyzer;
    private final ModuleDiscovery moduleDiscovery;
    private final GradleAnalyzer gradleAnalyzer;

    public RepositoryAnalysisService(
            RepositoryScanService repositoryScanService,
            JavaAnalysisService javaAnalysisService,
            RepositoryDiscovery repositoryDiscovery,
            ClientExtensionDiscovery clientExtensionDiscovery,
            ClientExtensionAnalyzer clientExtensionAnalyzer,
            ModuleDiscovery moduleDiscovery,
            GradleAnalyzer gradleAnalyzer) {

        this.repositoryScanService = repositoryScanService;
        this.javaAnalysisService = javaAnalysisService;
        this.repositoryDiscovery = repositoryDiscovery;
        this.clientExtensionDiscovery = clientExtensionDiscovery;
        this.clientExtensionAnalyzer = clientExtensionAnalyzer;
        this.moduleDiscovery = moduleDiscovery;
        this.gradleAnalyzer = gradleAnalyzer;
    }

    public RepositoryAnalysisResult analyzeRepository(
            Path repositoryPath) throws IOException {

        // Step 1: Scan repository
        FileDiscoveryResult fileDiscovery =
                repositoryScanService.scanRepository(repositoryPath);

        // Step 2: Analyze Java files
        List<Path> javaFiles = fileDiscovery.getCategorizedFiles()
                .getOrDefault("JAVA", List.of());

        JavaAnalysisResult javaAnalysis =
                javaAnalysisService.analyzeJavaFiles(javaFiles);

        // Step 3: Discover repository information
        RepositoryInfo repositoryInfo =
                repositoryDiscovery.discoverRepository(repositoryPath);

        // Step 4: Discover Client Extensions
        Path clientExtensionsPath =
                resolveClientExtensionsDirectory(repositoryPath);

        List<ClientExtensionInfo> extensions =
                clientExtensionDiscovery.discoverClientExtensions(
                        clientExtensionsPath
                );

        // Step 5: Analyze Client Extensions
        List<ClientExtensionAnalysis> clientExtensionResults =
                extensions.stream()
                        .map(clientExtensionAnalyzer::analyze)
                        .toList();

        // Step 6: Analyze workspace-level Gradle files
        List<GradleInfo> workspaceGradleFiles =
                analyzeWorkspaceGradleFiles(
                        repositoryInfo,
                        fileDiscovery
                );

        // Step 7: Analyze module-level Gradle files
        List<GradleInfo> moduleGradleFiles =
                analyzeModuleGradleFiles(repositoryInfo);

        // Step 8: Combine Gradle results
        GradleAnalysisResult gradleAnalysis =
                new GradleAnalysisResult(
                        workspaceGradleFiles,
                        moduleGradleFiles
                );

        // Step 9: Return complete repository analysis
        return new RepositoryAnalysisResult(
                fileDiscovery,
                javaAnalysis,
                clientExtensionResults,
                gradleAnalysis
        );
    }

    private List<GradleInfo> analyzeWorkspaceGradleFiles(
            RepositoryInfo repositoryInfo,
            FileDiscoveryResult fileDiscovery) {

        if (!repositoryInfo.isLiferayWorkspace()) {
            return List.of();
        }

        Path workspaceRoot = repositoryInfo.getRepositoryPath()
                .toAbsolutePath()
                .normalize();

        List<GradleInfo> results = new ArrayList<>();

        fileDiscovery.getCategorizedFiles()
                .forEach((category, files) -> {

                    if (!category.startsWith("GRADLE")) {
                        return;
                    }

                    for (Path file : files) {

                        if (!Files.isRegularFile(file)) {
                            continue;
                        }

                        Path parent = file.getParent();

                        if (parent == null
                                || !parent.toAbsolutePath()
                                .normalize()
                                .equals(workspaceRoot)) {
                            continue;
                        }

                        // Skip properties files and wrapper properties.
                        String fileName = file.getFileName().toString();

                        if (!fileName.endsWith(".gradle")) {
                            continue;
                        }

                        results.add(
                                gradleAnalyzer.analyzeGradleFile(file)
                        );
                    }
                });

        return List.copyOf(results);
    }

    private List<GradleInfo> analyzeModuleGradleFiles(
            RepositoryInfo repositoryInfo) {

        if (!repositoryInfo.isLiferayWorkspace()
                || repositoryInfo.getModulesDirectory() == null
                || !Files.isDirectory(
                repositoryInfo.getModulesDirectory())) {
            return List.of();
        }

        List<ModuleInfo> modules =
                moduleDiscovery.discoverModules(
                        repositoryInfo.getModulesDirectory()
                );

        return modules.stream()
                .map(ModuleInfo::getBuildFile)
                .map(gradleAnalyzer::analyzeGradleFile)
                .toList();
    }

    private Path resolveClientExtensionsDirectory(
            Path repositoryPath) {

        // Case 1: Supplied path itself is a Client Extension
        if (Files.isRegularFile(
                repositoryPath.resolve("client-extension.yaml"))) {
            return repositoryPath;
        }

        // Case 2: Supplied path is client-extensions directory
        if (repositoryPath.getFileName() != null
                && repositoryPath.getFileName().toString()
                .equals("client-extensions")) {
            return repositoryPath;
        }

        // Case 3: Workspace root
        Path clientExtensionsPath =
                repositoryPath.resolve("client-extensions");

        if (Files.isDirectory(clientExtensionsPath)) {
            return clientExtensionsPath;
        }

        // Case 4: Other directory
        return repositoryPath;
    }
}
