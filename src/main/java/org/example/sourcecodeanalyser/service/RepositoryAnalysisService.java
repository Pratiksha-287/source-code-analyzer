
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.analyzer.GradleAnalyzer;
import org.example.sourcecodeanalyser.analyzer.clientextension.ClientExtensionAnalyzer;
import org.example.sourcecodeanalyser.analyzer.configuration.ConfigurationAnalyzer;
import org.example.sourcecodeanalyser.analyzer.python.PythonFileAnalyzer;
import org.example.sourcecodeanalyser.discovery.ClientExtensionDiscovery;
import org.example.sourcecodeanalyser.discovery.ModuleDiscovery;
import org.example.sourcecodeanalyser.discovery.RepositoryDiscovery;
import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.example.sourcecodeanalyser.model.common.RepositoryInfo;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;
import org.example.sourcecodeanalyser.model.module.GradleInfo;
import org.example.sourcecodeanalyser.model.module.ModuleInfo;
import org.example.sourcecodeanalyser.model.python.PythonFileAnalysis;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RepositoryAnalysisService {

    private final RepositoryScanService repositoryScanService;
    private final JavaAnalysisService javaAnalysisService;
    private final RepositoryDiscovery repositoryDiscovery;
    private final ClientExtensionDiscovery clientExtensionDiscovery;
    private final ClientExtensionAnalyzer clientExtensionAnalyzer;
    private final ModuleDiscovery moduleDiscovery;
    private final GradleAnalyzer gradleAnalyzer;
    private final PythonFileAnalyzer pythonFileAnalyzer;
    private final ConfigurationAnalyzer configurationAnalyzer;

    public RepositoryAnalysisService(
            RepositoryScanService repositoryScanService,
            JavaAnalysisService javaAnalysisService,
            RepositoryDiscovery repositoryDiscovery,
            ClientExtensionDiscovery clientExtensionDiscovery,
            ClientExtensionAnalyzer clientExtensionAnalyzer,
            ModuleDiscovery moduleDiscovery,
            GradleAnalyzer gradleAnalyzer,
            PythonFileAnalyzer pythonFileAnalyzer,
            ConfigurationAnalyzer configurationAnalyzer) {

        this.repositoryScanService = repositoryScanService;
        this.javaAnalysisService = javaAnalysisService;
        this.repositoryDiscovery = repositoryDiscovery;
        this.clientExtensionDiscovery = clientExtensionDiscovery;
        this.clientExtensionAnalyzer = clientExtensionAnalyzer;
        this.moduleDiscovery = moduleDiscovery;
        this.gradleAnalyzer = gradleAnalyzer;
        this.pythonFileAnalyzer = pythonFileAnalyzer;
        this.configurationAnalyzer = configurationAnalyzer;
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

        // Step 9: Analyze Python files
        List<Path> pythonFiles = fileDiscovery.getCategorizedFiles()
                .getOrDefault("PYTHON", List.of());

        List<PythonFileAnalysis> pythonAnalysis =
                new ArrayList<>();

        for (Path pythonFile : pythonFiles) {
            pythonAnalysis.add(
                    pythonFileAnalyzer.analyze(pythonFile)
            );
        }

        // Step 10: Analyze configuration files
        List<ConfigurationFileAnalysis> configurationAnalysis =
                analyzeConfigurationFiles(
                        repositoryPath,
                        fileDiscovery
                );

        // Step 11: Return complete repository analysis
        return new RepositoryAnalysisResult(
                fileDiscovery,
                javaAnalysis,
                clientExtensionResults,
                gradleAnalysis,
                pythonAnalysis,
                configurationAnalysis
        );
    }


    private List<ConfigurationFileAnalysis> analyzeConfigurationFiles(
            Path repositoryPath,
            FileDiscoveryResult fileDiscovery) {

        Map<String, List<Path>> categorizedFiles =
                fileDiscovery.getCategorizedFiles();

        List<Path> configurationFiles = new ArrayList<>();

        configurationFiles.addAll(
                categorizedFiles.getOrDefault("YAML", List.of())
        );

        configurationFiles.addAll(
                categorizedFiles.getOrDefault("PROPERTIES", List.of())
        );

        configurationFiles.addAll(
                categorizedFiles.getOrDefault("ENV", List.of())
        );

        categorizedFiles.getOrDefault("DOCKER", List.of())
                .stream()
                .filter(this::isDockerfile)
                .forEach(configurationFiles::add);

        // Liferay OSGi configuration files may currently be categorized as OTHER.
        categorizedFiles.values()
                .stream()
                .flatMap(List::stream)
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName()
                        .toString()
                        .toLowerCase()
                        .endsWith(".config"))
                .forEach(configurationFiles::add);

        return configurationFiles.stream()
                .distinct()
                .map(file -> configurationAnalyzer.analyze(
                        file,
                        repositoryPath
                ))
                .toList();
    }


    private boolean isDockerfile(Path file) {

        String name = file.getFileName()
                .toString()
                .toLowerCase();

        return name.equals("dockerfile")
                || name.startsWith("dockerfile.");
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

        if (Files.isRegularFile(
                repositoryPath.resolve("client-extension.yaml"))) {
            return repositoryPath;
        }

        if (repositoryPath.getFileName() != null
                && repositoryPath.getFileName().toString()
                .equals("client-extensions")) {
            return repositoryPath;
        }

        Path clientExtensionsPath =
                repositoryPath.resolve("client-extensions");

        if (Files.isDirectory(clientExtensionsPath)) {
            return clientExtensionsPath;
        }

        return repositoryPath;
    }
}
