
package org.example.sourcecodeanalyser.service;

import org.example.sourcecodeanalyser.analyzer.GradleAnalyzer;
import org.example.sourcecodeanalyser.analyzer.clientextension.ClientExtensionAnalyzer;
import org.example.sourcecodeanalyser.analyzer.configuration.ConfigurationAnalyzer;
import org.example.sourcecodeanalyser.discovery.ClientExtensionDiscovery;
import org.example.sourcecodeanalyser.discovery.ModuleDiscovery;
import org.example.sourcecodeanalyser.discovery.RepositoryDiscovery;
import org.example.sourcecodeanalyser.model.FileDiscoveryResult;
import org.example.sourcecodeanalyser.model.clientextension.FrontendFileAnalysis;
import org.example.sourcecodeanalyser.model.JavaAnalysisResult;
import org.example.sourcecodeanalyser.model.RepositoryAnalysisResult;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionAnalysis;
import org.example.sourcecodeanalyser.model.clientextension.ClientExtensionInfo;
import org.example.sourcecodeanalyser.model.common.RepositoryInfo;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.example.sourcecodeanalyser.model.module.GradleAnalysisResult;
import org.example.sourcecodeanalyser.model.module.GradleInfo;
import org.example.sourcecodeanalyser.model.module.ModuleInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RepositoryAnalysisService {

    private static final Logger log =
            LoggerFactory.getLogger(RepositoryAnalysisService.class);

    private final RepositoryScanService repositoryScanService;
    private final JavaAnalysisService javaAnalysisService;
    private final RepositoryDiscovery repositoryDiscovery;
    private final ModuleDiscovery moduleDiscovery;
    private final GradleAnalyzer gradleAnalyzer;
    private final ClientExtensionDiscovery clientExtensionDiscovery;
    private final ClientExtensionAnalyzer clientExtensionAnalyzer;
    private final ConfigurationAnalyzer configurationAnalyzer;
    private final FrontendFileAnalysisService frontendFileAnalysisService;

    public RepositoryAnalysisService(
            RepositoryScanService repositoryScanService,
            JavaAnalysisService javaAnalysisService,
            RepositoryDiscovery repositoryDiscovery,
            ModuleDiscovery moduleDiscovery,
            GradleAnalyzer gradleAnalyzer,
            ClientExtensionDiscovery clientExtensionDiscovery,
            ClientExtensionAnalyzer clientExtensionAnalyzer,
            ConfigurationAnalyzer configurationAnalyzer,
            FrontendFileAnalysisService frontendFileAnalysisService) {

        this.repositoryScanService = repositoryScanService;
        this.javaAnalysisService = javaAnalysisService;
        this.repositoryDiscovery = repositoryDiscovery;
        this.moduleDiscovery = moduleDiscovery;
        this.gradleAnalyzer = gradleAnalyzer;
        this.clientExtensionDiscovery = clientExtensionDiscovery;
        this.clientExtensionAnalyzer = clientExtensionAnalyzer;
        this.configurationAnalyzer = configurationAnalyzer;
        this.frontendFileAnalysisService = frontendFileAnalysisService;
    }

    public RepositoryAnalysisResult analyzeRepository(
            Path repositoryPath) throws IOException {

        Path normalizedRoot =
                repositoryPath.toAbsolutePath().normalize();

        // 1. Discover repository and workspace information.
        RepositoryInfo discoveredRepository =
                repositoryDiscovery.discoverRepository(normalizedRoot);

        RepositoryInfo repositoryInfo =
                toRelativeRepositoryInfo(
                        discoveredRepository,
                        normalizedRoot
                );

        // 2. Discover and classify repository files.
        FileDiscoveryResult fileDiscovery =
                repositoryScanService.scanRepository(normalizedRoot);

        // 3. Analyze Java files.
        List<Path> javaFiles =
                fileDiscovery.getCategorizedFiles()
                        .getOrDefault("JAVA", List.of());

        JavaAnalysisResult javaAnalysis =
                javaAnalysisService.analyzeJavaFiles(
                        javaFiles,
                        normalizedRoot
                );

        // 4. Discover modules.
        List<ModuleInfo> discoveredModules =
                moduleDiscovery.discoverModules(
                        discoveredRepository.getModulesDirectory()
                );

        List<ModuleInfo> modules = discoveredModules.stream()
                .map(module -> toRelativeModuleInfo(module, normalizedRoot))
                .toList();

        // 5. Analyze workspace and module Gradle files.
        GradleAnalysisResult gradleAnalysis =
                analyzeGradleFiles(
                        normalizedRoot,
                        discoveredModules
                );

        // 6. Discover and analyze Client Extensions.
        List<ClientExtensionInfo> discoveredExtensions =
                clientExtensionDiscovery.discoverClientExtensions(
                        discoveredRepository.getClientExtensionsDirectory()
                );

        List<ClientExtensionAnalysis> clientExtensions =
                analyzeClientExtensions(discoveredExtensions);

        // 7. Analyze frontend files outside Client Extensions.
        List<FrontendFileAnalysis> frontendAnalysis =
                analyzeFrontendFiles(
                        fileDiscovery,
                        normalizedRoot,
                        discoveredExtensions
                );

        // 8. Analyze supported configuration files.
        List<ConfigurationFileAnalysis> configurationFiles =
                analyzeConfigurationFiles(
                        fileDiscovery,
                        normalizedRoot
                );

        // 9. Combine all analysis results.
        return new RepositoryAnalysisResult(
                repositoryInfo,
                fileDiscovery,
                javaAnalysis,
                frontendAnalysis,
                gradleAnalysis,
                modules,
                clientExtensions,
                configurationFiles
        );
    }

    private List<FrontendFileAnalysis> analyzeFrontendFiles(
            FileDiscoveryResult fileDiscovery,
            Path repositoryRoot,
            List<ClientExtensionInfo> discoveredExtensions) {

        List<Path> frontendFiles = Stream.of(
                        "JAVASCRIPT",
                        "TYPESCRIPT"
                )
                .flatMap(fileType ->
                        fileDiscovery.getCategorizedFiles()
                                .getOrDefault(fileType, List.of())
                                .stream()
                )
                .distinct()
                .filter(file -> !isInsideClientExtension(
                        file,
                        discoveredExtensions
                ))
                .sorted()
                .toList();

        if (frontendFiles.isEmpty()) {
            return List.of();
        }

        return frontendFileAnalysisService.analyzeFiles(
                frontendFiles,
                repositoryRoot
        );
    }

    private boolean isInsideClientExtension(
            Path file,
            List<ClientExtensionInfo> discoveredExtensions) {

        Path normalizedFile = file.toAbsolutePath().normalize();

        return discoveredExtensions.stream()
                .map(ClientExtensionInfo::getExtensionPath)
                .filter(path -> path != null)
                .map(path -> path.toAbsolutePath().normalize())
                .anyMatch(normalizedFile::startsWith);
    }

    private GradleAnalysisResult analyzeGradleFiles(
            Path repositoryRoot,
            List<ModuleInfo> modules) {

        List<GradleInfo> workspaceGradleFiles = new ArrayList<>();
        List<GradleInfo> moduleGradleFiles = new ArrayList<>();

        // Analyze the root workspace build file, if present.
        Path workspaceBuildFile = findBuildFile(repositoryRoot);

        if (workspaceBuildFile != null) {
            try {
                workspaceGradleFiles.add(
                        toRelativeGradleInfo(
                                gradleAnalyzer.analyzeGradleFile(
                                        workspaceBuildFile
                                ),
                                repositoryRoot
                        )
                );

            } catch (Exception exception) {
                log.warn(
                        "Skipping workspace Gradle file {} because analysis failed",
                        workspaceBuildFile,
                        exception
                );
            }
        }

        // Analyze each discovered module build file.
        for (ModuleInfo module : modules) {

            Path buildFile = module.getBuildFile();

            if (buildFile == null) {
                continue;
            }

            try {
                moduleGradleFiles.add(
                        toRelativeGradleInfo(
                                gradleAnalyzer.analyzeGradleFile(buildFile),
                                repositoryRoot
                        )
                );

            } catch (Exception exception) {
                log.warn(
                        "Skipping module Gradle file {} because analysis failed",
                        buildFile,
                        exception
                );
            }
        }

        return new GradleAnalysisResult(
                workspaceGradleFiles,
                moduleGradleFiles
        );
    }

    private Path findBuildFile(Path directory) {

        Path groovyBuildFile = directory.resolve("build.gradle");

        if (Files.isRegularFile(groovyBuildFile)) {
            return groovyBuildFile;
        }

        Path kotlinBuildFile = directory.resolve("build.gradle.kts");

        if (Files.isRegularFile(kotlinBuildFile)) {
            return kotlinBuildFile;
        }

        return null;
    }

    private List<ClientExtensionAnalysis> analyzeClientExtensions(
            List<ClientExtensionInfo> discoveredExtensions) {

        List<ClientExtensionAnalysis> results = new ArrayList<>();

        for (ClientExtensionInfo extension : discoveredExtensions) {

            try {
                results.add(
                        clientExtensionAnalyzer.analyze(extension)
                );

            } catch (Exception exception) {
                log.warn(
                        "Skipping Client Extension {} because analysis failed",
                        extension.getExtensionName(),
                        exception
                );
            }
        }

        return results;
    }

    private List<ConfigurationFileAnalysis> analyzeConfigurationFiles(
            FileDiscoveryResult fileDiscovery,
            Path repositoryRoot) {

        List<Path> configurationPaths =
                fileDiscovery.getCategorizedFiles()
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .distinct()
                        .filter(this::isSupportedConfigurationFile)
                        .filter(file ->
                                !file.getFileName()
                                        .toString()
                                        .equalsIgnoreCase("client-extension.yaml")
                        )
                        .sorted()
                        .toList();

        List<ConfigurationFileAnalysis> results =
                new ArrayList<>();

        for (Path file : configurationPaths) {

            try {
                results.add(
                        configurationAnalyzer.analyze(
                                file,
                                repositoryRoot
                        )
                );

            } catch (Exception exception) {
                log.warn(
                        "Skipping configuration file {} because analysis failed",
                        file,
                        exception
                );
            }
        }

        return results;
    }

    private boolean isSupportedConfigurationFile(Path file) {

        String fileName = file.getFileName()
                .toString()
                .toLowerCase();

        return fileName.endsWith(".yaml")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".config")
                || fileName.equals("dockerfile")
                || fileName.startsWith("dockerfile.")
                || fileName.equals(".env")
                || fileName.startsWith(".env.");
    }

    private RepositoryInfo toRelativeRepositoryInfo(
            RepositoryInfo repository,
            Path repositoryRoot) {

        return new RepositoryInfo(
                repository.getRepositoryName(),
                Path.of("."),
                toRelativePath(repositoryRoot, repository.getModulesDirectory()),
                toRelativePath(repositoryRoot, repository.getClientExtensionsDirectory()),
                toRelativePath(repositoryRoot, repository.getConfigsDirectory()),
                toRelativePath(repositoryRoot, repository.getThemesDirectory()),
                toRelativePath(repositoryRoot, repository.getWarsDirectory()),
                repository.isLiferayWorkspace()
        );
    }

    private ModuleInfo toRelativeModuleInfo(
            ModuleInfo module,
            Path repositoryRoot) {

        return new ModuleInfo(
                module.getModuleName(),
                toRelativePath(repositoryRoot, module.getModulePath()),
                toRelativePath(repositoryRoot, module.getBuildFile()),
                toRelativePath(repositoryRoot, module.getBndFile())
        );
    }

    private GradleInfo toRelativeGradleInfo(
            GradleInfo gradleInfo,
            Path repositoryRoot) {

        return new GradleInfo(
                gradleInfo.getModuleName(),
                toRelativePath(repositoryRoot, gradleInfo.getBuildFilePath()),
                gradleInfo.getGroup(),
                gradleInfo.getPlugins(),
                gradleInfo.getDependencies(),
                gradleInfo.getRepositories()
        );
    }

    private Path toRelativePath(
            Path repositoryRoot,
            Path path) {

        if (path == null) {
            return null;
        }

        Path normalizedRoot =
                repositoryRoot.toAbsolutePath().normalize();

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        if (!normalizedPath.startsWith(normalizedRoot)) {
            return normalizedPath;
        }

        return normalizedRoot.relativize(normalizedPath);
    }
}
