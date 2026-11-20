
package org.example.sourcecodeanalyser.scanner;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileDiscoveryService {

    private static final Map<String, String> FILE_TYPES =
            createFileTypes();

    private static final Map<String, String> SPECIAL_FILES =
            createSpecialFiles();

    // Initialize supported file extensions
    private static Map<String, String> createFileTypes() {

        Map<String, String> fileTypes = new LinkedHashMap<>();

        // Java and JVM languages
        fileTypes.put(".gradle.kts", "GRADLE_KOTLIN");
        fileTypes.put(".java", "JAVA");
        fileTypes.put(".gradle", "GRADLE");
        fileTypes.put(".kt", "KOTLIN");
        fileTypes.put(".kts", "KOTLIN");
        fileTypes.put(".scala", "SCALA");
        fileTypes.put(".sc", "SCALA");

        // Python
        fileTypes.put(".py", "PYTHON");
        fileTypes.put(".pyi", "PYTHON");
        fileTypes.put(".pyx", "PYTHON");

        // JavaScript
        fileTypes.put(".mjs", "JAVASCRIPT");
        fileTypes.put(".cjs", "JAVASCRIPT");
        fileTypes.put(".jsx", "JAVASCRIPT");
        fileTypes.put(".js", "JAVASCRIPT");

        // TypeScript
        fileTypes.put(".mts", "TYPESCRIPT");
        fileTypes.put(".cts", "TYPESCRIPT");
        fileTypes.put(".tsx", "TYPESCRIPT");
        fileTypes.put(".ts", "TYPESCRIPT");

        // C and C++
        fileTypes.put(".cpp", "CPP");
        fileTypes.put(".cxx", "CPP");
        fileTypes.put(".cc", "CPP");
        fileTypes.put(".hpp", "CPP");
        fileTypes.put(".hxx", "CPP");
        fileTypes.put(".hh", "CPP");
        fileTypes.put(".c", "C");
        fileTypes.put(".h", "C");

        // Other backend languages
        fileTypes.put(".cs", "CSHARP");
        fileTypes.put(".go", "GO");
        fileTypes.put(".rs", "RUST");
        fileTypes.put(".php", "PHP");
        fileTypes.put(".rb", "RUBY");
        fileTypes.put(".swift", "SWIFT");
        fileTypes.put(".dart", "DART");
        fileTypes.put(".r", "R");

        // Frontend technologies
        fileTypes.put(".vue", "VUE");
        fileTypes.put(".svelte", "SVELTE");
        fileTypes.put(".html", "HTML");
        fileTypes.put(".htm", "HTML");
        fileTypes.put(".scss", "STYLESHEETS");
        fileTypes.put(".sass", "STYLESHEETS");
        fileTypes.put(".less", "STYLESHEETS");
        fileTypes.put(".css", "CSS");

        // Configuration files
        fileTypes.put(".properties", "PROPERTIES");
        fileTypes.put(".xml", "XML");
        fileTypes.put(".xsd", "XML");
        fileTypes.put(".xsl", "XML");
        fileTypes.put(".wsdl", "XML");
        fileTypes.put(".yaml", "YAML");
        fileTypes.put(".yml", "YAML");
        fileTypes.put(".jsonc", "JSON");
        fileTypes.put(".json5", "JSON");
        fileTypes.put(".json", "JSON");
        fileTypes.put(".toml", "TOML");
        fileTypes.put(".ini", "CONFIG");
        fileTypes.put(".cfg", "CONFIG");
        fileTypes.put(".conf", "CONFIG");

        // Database files
        fileTypes.put(".sql", "SQL");

        // Infrastructure and DevOps
        fileTypes.put(".tfvars", "TERRAFORM");
        fileTypes.put(".tf", "TERRAFORM");
        fileTypes.put(".hcl", "TERRAFORM");

        // Shell scripts
        fileTypes.put(".bash", "SHELL");
        fileTypes.put(".zsh", "SHELL");
        fileTypes.put(".sh", "SHELL");

        fileTypes.put(".psm1", "POWERSHELL");
        fileTypes.put(".ps1", "POWERSHELL");

        fileTypes.put(".bat", "WINDOWS_SCRIPT");
        fileTypes.put(".cmd", "WINDOWS_SCRIPT");

        // Documentation files
        fileTypes.put(".mdx", "MARKDOWN");
        fileTypes.put(".md", "MARKDOWN");
        fileTypes.put(".rst", "DOCUMENTATION");
        fileTypes.put(".adoc", "DOCUMENTATION");
        fileTypes.put(".txt", "DOCUMENTATION");

        return Collections.unmodifiableMap(fileTypes);
    }

    // Initialize special files without conventional extensions
    private static Map<String, String> createSpecialFiles() {

        Map<String, String> specialFiles = new LinkedHashMap<>();

        specialFiles.put("dockerfile", "DOCKER");
        specialFiles.put("jenkinsfile", "JENKINS");
        specialFiles.put("makefile", "BUILD");
        specialFiles.put("procfile", "CONFIG");

        specialFiles.put(".gitignore", "GIT");
        specialFiles.put(".gitattributes", "GIT");
        specialFiles.put(".gitmodules", "GIT");

        specialFiles.put(".dockerignore", "DOCKER");
        specialFiles.put(".editorconfig", "CONFIG");

        // Gradle configuration files
        specialFiles.put("settings.gradle", "GRADLE_SETTINGS");
        specialFiles.put("build.gradle", "GRADLE_BUILD");

        specialFiles.put("settings.gradle.kts", "GRADLE_SETTINGS_KOTLIN");
        specialFiles.put("build.gradle.kts", "GRADLE_BUILD_KOTLIN");

        specialFiles.put("gradle.properties", "GRADLE_PROPERTIES");
        specialFiles.put("gradle-wrapper.properties", "GRADLE_WRAPPER_PROPERTIES");
        //

        return Collections.unmodifiableMap(specialFiles);
    }

    // Classify discovered files into categories
    public Map<String, List<Path>> classifyFiles(List<Path> files) {

        Map<String, List<Path>> categorizedFiles =
                new LinkedHashMap<>();

        // Initialize extension-based categories
        for (String fileType : FILE_TYPES.values()) {
            categorizedFiles.putIfAbsent(
                    fileType,
                    new ArrayList<>()
            );
        }

        // Initialize special-file categories
        for (String fileType : SPECIAL_FILES.values()) {
            categorizedFiles.putIfAbsent(
                    fileType,
                    new ArrayList<>()
            );
        }

        // Initialize OTHER category
        categorizedFiles.put("OTHER", new ArrayList<>());

        // Process each discovered file
        for (Path file : files) {

            String fileName = file.getFileName()
                    .toString()
                    .toLowerCase(Locale.ROOT);

            // Check special filenames first
            String detectedType = SPECIAL_FILES.get(fileName);

            // If not a special file, check extensions
            if (detectedType == null) {

                detectedType = FILE_TYPES.entrySet()
                        .stream()
                        .filter(entry ->
                                fileName.endsWith(entry.getKey()))
                        .max((first, second) ->
                                Integer.compare(
                                        first.getKey().length(),
                                        second.getKey().length()
                                ))
                        .map(Map.Entry::getValue)
                        .orElse("OTHER");
            }

            // Add file to its respective category
            categorizedFiles.get(detectedType).add(file);
        }

        return categorizedFiles;
    }
}
