
package org.example.sourcecodeanalyser.analyzer;

import org.example.sourcecodeanalyser.model.module.GradleDependencyInfo;
import org.example.sourcecodeanalyser.model.module.GradleInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GradleAnalyzer {

    public GradleInfo analyzeGradleFile(Path buildFile) {

        if (buildFile == null || !Files.isRegularFile(buildFile)) {
            throw new IllegalArgumentException(
                    "Invalid Gradle build file: " + buildFile
            );
        }

        try {
            String content = Files.readString(buildFile);

            Path parent = buildFile.getParent();

            String moduleName = parent == null
                    ? buildFile.getFileName().toString()
                    : parent.getFileName().toString();

            String group = extractGroup(content);
            List<String> plugins = extractPlugins(content);
            List<GradleDependencyInfo> dependencies =
                    extractDependencies(content);
            List<String> repositories = extractRepositories(content);

            return new GradleInfo(
                    moduleName,
                    buildFile,
                    group,
                    plugins,
                    dependencies,
                    repositories
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to analyze Gradle file: " + buildFile, e
            );
        }
    }

    // --------------------------------------------------
    // GROUP
    // --------------------------------------------------

    private String extractGroup(String content) {

        Pattern pattern = Pattern.compile(
                "(?m)^\\s*group\\s*=\\s*['\"]([^'\"]+)['\"]"
        );

        Matcher matcher = pattern.matcher(content);

        return matcher.find() ? matcher.group(1) : null;
    }

    // --------------------------------------------------
    // PLUGINS
    // --------------------------------------------------

    private List<String> extractPlugins(String content) {

        Set<String> plugins = new LinkedHashSet<>();

        // plugins { id("plugin.id") version "1.0" }
        Pattern pluginsBlockPattern = Pattern.compile(
                "\\bid\\s*\\(?\\s*['\"]([^'\"]+)['\"]"
        );

        Matcher blockMatcher = Pattern.compile(
                "\\bplugins\\s*\\{"
        ).matcher(content);

        while (blockMatcher.find()) {

            String block = extractBlockContent(
                    content,
                    blockMatcher.end() - 1
            );

            if (block == null) {
                continue;
            }

            Matcher pluginMatcher =
                    pluginsBlockPattern.matcher(block);

            while (pluginMatcher.find()) {
                plugins.add(pluginMatcher.group(1));
            }
        }

        // apply plugin: "com.liferay.workspace"
        Pattern applyPluginPattern = Pattern.compile(
                "\\bapply\\s+plugin\\s*:\\s*['\"]([^'\"]+)['\"]"
        );

        Matcher applyMatcher = applyPluginPattern.matcher(content);

        while (applyMatcher.find()) {
            plugins.add(applyMatcher.group(1));
        }

        return new ArrayList<>(plugins);
    }


    // --------------------------------------------------
    // DEPENDENCIES
    // --------------------------------------------------

    private List<GradleDependencyInfo> extractDependencies(
            String content
    ) {

        List<GradleDependencyInfo> dependencies = new ArrayList<>();

        StringBuilder projectContent = new StringBuilder(content);

        // STEP 1: Find buildscript blocks
        Matcher buildscriptMatcher = Pattern.compile(
                "\\bbuildscript\\s*\\{"
        ).matcher(content);

        while (buildscriptMatcher.find()) {

            int openingBrace = buildscriptMatcher.end() - 1;

            String buildscriptBlock = extractBlockContent(
                    content,
                    openingBrace
            );

            if (buildscriptBlock == null) {
                continue;
            }

            // Extract dependencies belonging to buildscript
            extractDependencyBlocks(
                    buildscriptBlock,
                    dependencies,
                    "BUILDSCRIPT"
            );

            // Exclude this entire buildscript block
            // before searching for project dependencies
            int closingBrace = openingBrace + 1
                    + buildscriptBlock.length();

            projectContent.replace(
                    buildscriptMatcher.start(),
                    closingBrace + 1,
                    " ".repeat(
                            closingBrace + 1 - buildscriptMatcher.start()
                    )
            );
        }

        // STEP 2: Find remaining project dependencies
        extractDependencyBlocks(
                projectContent.toString(),
                dependencies,
                "PROJECT"
        );

        return dependencies;
    }

    // Find dependencies blocks and extract their contents
    private void extractDependencyBlocks(
            String content,
            List<GradleDependencyInfo> dependencies,
            String scope
    ) {

        Matcher blockMatcher = Pattern.compile(
                "\\bdependencies\\s*\\{"
        ).matcher(content);

        while (blockMatcher.find()) {

            String dependencyBlock = extractBlockContent(
                    content,
                    blockMatcher.end() - 1
            );

            if (dependencyBlock == null) {
                continue;
            }

            extractShortNotation(
                    dependencyBlock,
                    dependencies,
                    scope
            );

            extractMapNotation(
                    dependencyBlock,
                    dependencies,
                    scope
            );

            extractProjectDependencies(
                    dependencyBlock,
                    dependencies,
                    scope
            );
        }
    }

    // implementation "group:artifact:version"
    private void extractShortNotation(
            String content,
            List<GradleDependencyInfo> dependencies,
            String scope
    ) {

        Pattern pattern = Pattern.compile(
                "(\\w+)\\s*(?:\\(\\s*)?['\"]"
                        + "([^:'\"]+):([^:'\"]+)"
                        + "(?::([^'\"]+))?['\"]"
        );

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {

            dependencies.add(new GradleDependencyInfo(
                    matcher.group(1),
                    "EXTERNAL",
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    null,
                    scope
            ));
        }
    }

    // classpath group: "...", name: "...", version: "..."
    private void extractMapNotation(
            String content,
            List<GradleDependencyInfo> dependencies,
            String scope
    ) {

        Pattern pattern = Pattern.compile(
                "(\\w+)\\s+group\\s*:\\s*['\"]([^'\"]+)['\"]"
                        + "\\s*,\\s*name\\s*:\\s*['\"]([^'\"]+)['\"]"
                        + "(?:\\s*,\\s*version\\s*:\\s*['\"]([^'\"]+)['\"])?"
        );

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {

            dependencies.add(new GradleDependencyInfo(
                    matcher.group(1),
                    "EXTERNAL",
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    null,
                    scope
            ));
        }
    }

    // implementation project(":some-module")
    private void extractProjectDependencies(
            String content,
            List<GradleDependencyInfo> dependencies,
            String scope
    ) {

        Pattern pattern = Pattern.compile(
                "(\\w+)\\s+project\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
        );

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {

            dependencies.add(new GradleDependencyInfo(
                    matcher.group(1),
                    "PROJECT",
                    null,
                    null,
                    null,
                    matcher.group(2),
                    scope
            ));
        }
    }


    // --------------------------------------------------
    // REPOSITORIES
    // --------------------------------------------------

    private List<String> extractRepositories(String content) {

        Set<String> repositories = new LinkedHashSet<>();

        Pattern standardPattern = Pattern.compile(
                "\\b(mavenCentral|mavenLocal|google)\\s*\\(\\s*\\)"
        );

        Matcher standardMatcher = standardPattern.matcher(content);

        while (standardMatcher.find()) {
            repositories.add(standardMatcher.group(1) + "()");
        }

        // Custom Maven repository blocks
        Matcher mavenBlockMatcher = Pattern.compile(
                "\\bmaven\\s*\\{"
        ).matcher(content);

        while (mavenBlockMatcher.find()) {

            String repositoryBlock = extractBlockContent(
                    content,
                    mavenBlockMatcher.end() - 1
            );

            if (repositoryBlock == null) {
                continue;
            }

            Matcher urlMatcher = Pattern.compile(
                    "\\burl\\s*(?:=\\s*)?['\"]([^'\"]+)['\"]"
            ).matcher(repositoryBlock);

            if (urlMatcher.find()) {
                repositories.add("maven: " + urlMatcher.group(1));
            } else {
                repositories.add("maven: custom repository");
            }
        }

        return new ArrayList<>(repositories);
    }

    // --------------------------------------------------
    // NESTED BLOCK EXTRACTION
    // --------------------------------------------------

    private String extractBlockContent(
            String content,
            int openingBrace
    ) {

        if (openingBrace < 0
                || openingBrace >= content.length()
                || content.charAt(openingBrace) != '{') {
            return null;
        }

        int depth = 0;

        char quote = 0;
        boolean escaped = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = openingBrace; i < content.length(); i++) {

            char current = content.charAt(i);

            char next = i + 1 < content.length()
                    ? content.charAt(i + 1)
                    : '\0';

            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                }
                continue;
            }

            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }

            if (quote != 0) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (current == '\\') {
                    escaped = true;
                    continue;
                }

                if (current == quote) {
                    quote = 0;
                }

                continue;
            }

            if (current == '/' && next == '/') {
                lineComment = true;
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }

            if (current == '\'' || current == '"') {
                quote = current;
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {

                depth--;

                if (depth == 0) {
                    return content.substring(
                            openingBrace + 1,
                            i
                    );
                }
            }
        }

        return null;
    }
}
