
package org.example.sourcecodeanalyser.analyzer.configuration;

import org.example.sourcecodeanalyser.model.configuration.ConfigurationFileAnalysis;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationKeyAnalysis;
import org.example.sourcecodeanalyser.model.configuration.ConfigurationReference;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConfigurationAnalyzer {

    private static final Pattern ENV_PATTERN = Pattern.compile(
            "^\\s*(?:export\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*="
    );

    private static final Pattern DOCKER_ARG_PATTERN = Pattern.compile(
            "^\\s*ARG\\s+([A-Za-z_][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DOCKER_ENV_ASSIGNMENT_PATTERN =
            Pattern.compile("(?:^|\\s)([A-Za-z_][A-Za-z0-9_]*)=");

    private static final Pattern DOCKER_ENV_LEGACY_PATTERN =
            Pattern.compile(
                    "^\\s*ENV\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PROPERTY_KEY_PATTERN =
            Pattern.compile("^\\s*([^#!\\s][^=:\\s]*?)\\s*[=:\\s]");

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[-_]?key|credential|private[-_]?key|access[-_]?key)"
    );

    private static final Set<String> REFERENCE_KEYS = Set.of(
            "include",
            "includes",
            "include-and-override",
            "spring.config.import",
            "spring.config.location",
            "config.file",
            "config.files",
            "import",
            "imports",
            "extends"
    );

    private record YamlKeyData(
            List<String> keys,
            Map<String, Integer> declarationCounts
    ) {
    }

    public ConfigurationFileAnalysis analyze(Path file) {
        return analyze(file, null);
    }

    public ConfigurationFileAnalysis analyze(
            Path file,
            Path repositoryRoot) {

        String filePath = resolveFilePath(file, repositoryRoot);
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String fileType = detectFileType(fileName);
        String environment = detectEnvironment(filePath);

        if (!Files.isRegularFile(file)) {
            return failure(
                    filePath,
                    fileType,
                    environment,
                    "Configuration file does not exist or is not a regular file."
            );
        }

        try {
            String content = Files.readString(file);

            if (content.isBlank()) {
                return result(
                        filePath, fileType, environment,
                        List.of(), List.of(), List.of(),
                        "EMPTY", List.of("Configuration file is empty.")
                );
            }

            if ("UNKNOWN".equals(fileType)) {
                return result(
                        filePath, fileType, environment,
                        List.of(), List.of(), List.of(),
                        "UNSUPPORTED",
                        List.of("Configuration file format is not supported.")
                );
            }

            YamlKeyData yamlKeyData = "YAML".equals(fileType)
                    ? analyzeYaml(content)
                    : null;

            List<String> keys = switch (fileType) {
                case "YAML" -> yamlKeyData.keys();
                case "PROPERTIES", "OSGI_CONFIG" ->
                        extractPropertiesKeys(file);
                case "ENV" -> extractEnvKeys(file);
                case "DOCKER" -> extractDockerKeys(file);
                default -> List.of();
            };

            List<String> diagnostics = new ArrayList<>();

            Map<String, Integer> declarationCounts =
                    "YAML".equals(fileType)
                            ? yamlKeyData.declarationCounts()
                            : countDeclarations(content, fileType);

            List<ConfigurationKeyAnalysis> keyDetails =
                    buildKeyDetails(
                            keys,
                            content,
                            fileType,
                            declarationCounts
                    );

            for (Map.Entry<String, Integer> entry :
                    declarationCounts.entrySet()) {

                if (entry.getValue() > 1) {
                    diagnostics.add(
                            "Repeated declaration detected for key: "
                                    + entry.getKey()
                    );
                }
            }

            List<ConfigurationReference> references =
                    extractReferences(
                            file,
                            content,
                            repositoryRoot,
                            fileType
                    );

            if (keys.isEmpty()) {
                diagnostics.add(
                        "No configuration keys were recognized in this file."
                );
            }

            String status = keys.isEmpty()
                    ? "NO_KEYS"
                    : diagnostics.isEmpty()
                    ? "PARSED"
                    : "PARSED_WITH_WARNINGS";

            return result(
                    filePath,
                    fileType,
                    environment,
                    keys,
                    keyDetails,
                    references,
                    status,
                    diagnostics
            );

        } catch (Exception exception) {
            return failure(
                    filePath,
                    fileType,
                    environment,
                    "Configuration analysis failed: "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private String resolveFilePath(Path file, Path repositoryRoot) {

        if (repositoryRoot != null) {
            try {
                return repositoryRoot.toAbsolutePath()
                        .normalize()
                        .relativize(file.toAbsolutePath().normalize())
                        .toString()
                        .replace('\\', '/');
            } catch (IllegalArgumentException ignored) {
                // Different filesystem roots; use the absolute path.
            }
        }

        return file.toString().replace('\\', '/');
    }

    private String detectFileType(String fileName) {

        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return "YAML";
        }

        if (fileName.endsWith(".properties")) {
            return "PROPERTIES";
        }

        if (fileName.endsWith(".config")) {
            return "OSGI_CONFIG";
        }

        if (fileName.equals("dockerfile")
                || fileName.startsWith("dockerfile.")) {
            return "DOCKER";
        }

        if (fileName.equals(".env") || fileName.startsWith(".env.")) {
            return "ENV";
        }

        return "UNKNOWN";
    }

    private String detectEnvironment(String filePath) {

        String normalized = filePath.toLowerCase(Locale.ROOT);
        String[] segments = normalized.split("[/\\\\]");

        for (String segment : segments) {
            switch (segment) {
                case "dev", "development", "local" -> {
                    return "DEVELOPMENT";
                }
                case "uat" -> {
                    return "UAT";
                }
                case "prod", "production" -> {
                    return "PRODUCTION";
                }
                case "staging" -> {
                    return "STAGING";
                }
                case "test", "testing" -> {
                    return "TEST";
                }
                case "common", "shared" -> {
                    return "COMMON";
                }
                default -> {
                    // Continue checking path segments.
                }
            }
        }

        return "COMMON";
    }

    private YamlKeyData analyzeYaml(String content) {

        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(3 * 1024 * 1024);
        options.setMaxAliasesForCollections(50);
        options.setAllowDuplicateKeys(true);

        Yaml yaml = new Yaml(new SafeConstructor(options));

        Set<String> keys = new LinkedHashSet<>();
        Map<String, Integer> declarationCounts = new LinkedHashMap<>();

        try {
            Node root = yaml.compose(new StringReader(content));

            if (root != null) {
                Set<Node> ancestors = Collections.newSetFromMap(
                        new IdentityHashMap<>()
                );

                extractYamlNode(
                        root,
                        "",
                        keys,
                        declarationCounts,
                        ancestors
                );
            }

        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Unable to parse YAML: " + exception.getMessage(),
                    exception
            );
        }

        return new YamlKeyData(
                List.copyOf(keys),
                Map.copyOf(declarationCounts)
        );
    }

    private void extractYamlNode(
            Node node,
            String parentPath,
            Set<String> keys,
            Map<String, Integer> declarationCounts,
            Set<Node> ancestors) {

        if (!ancestors.add(node)) {
            return;
        }

        try {

            if (node instanceof MappingNode mappingNode) {

                for (NodeTuple tuple : mappingNode.getValue()) {

                    Node keyNode = tuple.getKeyNode();

                    if (!(keyNode instanceof ScalarNode scalarKey)) {
                        continue;
                    }

                    String key = scalarKey.getValue();

                    String fullPath = parentPath.isEmpty()
                            ? key
                            : parentPath + "." + key;

                    keys.add(fullPath);

                    declarationCounts.merge(
                            fullPath,
                            1,
                            Integer::sum
                    );

                    extractYamlNode(
                            tuple.getValueNode(),
                            fullPath,
                            keys,
                            declarationCounts,
                            ancestors
                    );
                }

            } else if (node instanceof SequenceNode sequenceNode) {

                List<Node> elements = sequenceNode.getValue();

                for (int index = 0; index < elements.size(); index++) {

                    String itemPath = parentPath + "[" + index + "]";

                    extractYamlNode(
                            elements.get(index),
                            itemPath,
                            keys,
                            declarationCounts,
                            ancestors
                    );
                }
            }

        } finally {
            ancestors.remove(node);
        }
    }

    private List<String> extractPropertiesKeys(Path file)
            throws IOException {

        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        }

        return properties.stringPropertyNames()
                .stream()
                .sorted()
                .toList();
    }

    private List<String> extractEnvKeys(Path file)
            throws IOException {

        List<String> keys = new ArrayList<>();

        for (String line : Files.readAllLines(file)) {

            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher matcher = ENV_PATTERN.matcher(line);

            if (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }

        return List.copyOf(keys);
    }

    private List<String> extractDockerKeys(Path file)
            throws IOException {

        List<String> keys = new ArrayList<>();

        List<String> logicalLines =
                readDockerLogicalLines(Files.readAllLines(file));

        for (String line : logicalLines) {

            Matcher arg = DOCKER_ARG_PATTERN.matcher(line);

            if (arg.find()) {
                keys.add(arg.group(1));
                continue;
            }

            if (line.trim().toUpperCase(Locale.ROOT).startsWith("ENV ")) {

                String declaration =
                        line.trim().substring(4).trim();

                Matcher env =
                        DOCKER_ENV_ASSIGNMENT_PATTERN.matcher(declaration);

                if (env.find()) {
                    keys.add(env.group(1));
                } else {

                    Matcher legacy =
                            DOCKER_ENV_LEGACY_PATTERN.matcher(line);

                    if (legacy.find()) {
                        keys.add(legacy.group(1));
                    }
                }
            }
        }

        return List.copyOf(keys);
    }

    private List<String> readDockerLogicalLines(List<String> lines) {

        List<String> logicalLines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {

            String trimmed = line.stripTrailing();

            if (current.length() > 0) {
                current.append(' ');
            }

            if (trimmed.endsWith("\\")) {
                current.append(
                        trimmed.substring(0, trimmed.length() - 1)
                );
            } else {
                current.append(trimmed);
                logicalLines.add(current.toString());
                current.setLength(0);
            }
        }

        if (current.length() > 0) {
            logicalLines.add(current.toString());
        }

        return logicalLines;
    }

    private Map<String, Integer> countDeclarations(
            String content,
            String fileType) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String line : content.split("\\R")) {

            String key = null;

            switch (fileType) {

                case "PROPERTIES", "OSGI_CONFIG" -> {
                    Matcher matcher = PROPERTY_KEY_PATTERN.matcher(line);

                    if (matcher.find()) {
                        key = matcher.group(1).trim();
                    }
                }

                case "ENV" -> {
                    Matcher matcher = ENV_PATTERN.matcher(line);

                    if (matcher.find()) {
                        key = matcher.group(1);
                    }
                }

                case "DOCKER" -> {

                    Matcher arg = DOCKER_ARG_PATTERN.matcher(line);

                    if (arg.find()) {
                        key = arg.group(1);

                    } else if (line.trim().toUpperCase(Locale.ROOT)
                            .startsWith("ENV ")) {

                        String declaration =
                                line.trim().substring(4).trim();

                        Matcher env =
                                DOCKER_ENV_ASSIGNMENT_PATTERN.matcher(
                                        declaration
                                );

                        if (env.find()) {
                            key = env.group(1);

                        } else {

                            Matcher legacy =
                                    DOCKER_ENV_LEGACY_PATTERN.matcher(line);

                            if (legacy.find()) {
                                key = legacy.group(1);
                            }
                        }
                    }
                }

                default -> {
                    // YAML is handled by the Node-based parser.
                }
            }

            if (key != null && !key.isBlank()) {
                counts.merge(key, 1, Integer::sum);
            }
        }

        return counts;
    }

    private List<ConfigurationKeyAnalysis> buildKeyDetails(
            List<String> keys,
            String content,
            String fileType,
            Map<String, Integer> declarationCounts) {

        List<ConfigurationKeyAnalysis> details = new ArrayList<>();

        for (String key : keys) {

            String parent = findParentKey(key);
            String leaf = findLeafKey(key);

            String description = findDescription(
                    content,
                    leaf,
                    fileType
            );

            boolean sensitive = SECRET_PATTERN.matcher(key).find();

            boolean duplicate = "YAML".equals(fileType)
                    ? declarationCounts.getOrDefault(key, 0) > 1
                    : declarationCounts.getOrDefault(key, 0) > 1
                    || declarationCounts.getOrDefault(leaf, 0) > 1;

            details.add(new ConfigurationKeyAnalysis(
                    key,
                    parent,
                    description,
                    sensitive,
                    duplicate
            ));
        }

        return List.copyOf(details);
    }

    private String findParentKey(String key) {

        int dot = key.lastIndexOf('.');

        if (dot < 0) {
            return null;
        }

        return key.substring(0, dot);
    }

    private String findLeafKey(String key) {

        int dot = key.lastIndexOf('.');
        String leaf = dot >= 0 ? key.substring(dot + 1) : key;

        int bracket = leaf.indexOf('[');

        if (bracket >= 0) {
            leaf = leaf.substring(0, bracket);
        }

        return leaf;
    }

    private String findDescription(
            String content,
            String key,
            String fileType) {

        String[] lines = content.split("\\R");
        StringBuilder comments = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {

            String line = lines[index].trim();

            boolean declarationMatches = switch (fileType) {

                case "YAML" -> line.startsWith(key + ":")
                        || line.startsWith("'" + key + "':")
                        || line.startsWith("\"" + key + "\":");

                case "ENV" -> line.matches(
                        "(?i)^export\\s+" + Pattern.quote(key) + "\\s*=.*"
                ) || line.matches(
                        "^" + Pattern.quote(key) + "\\s*=.*"
                );

                case "DOCKER" -> line.matches(
                        "(?i)^(ARG|ENV)\\s+.*\\b"
                                + Pattern.quote(key) + "(\\s|=|$).*"
                );

                default -> line.startsWith(key + "=")
                        || line.startsWith(key + ":")
                        || line.startsWith(key + " ");
            };

            if (!declarationMatches) {
                continue;
            }

            List<String> preceding = new ArrayList<>();

            for (int previous = index - 1; previous >= 0; previous--) {

                String comment = lines[previous].trim();

                if (comment.isEmpty()) {
                    if (!preceding.isEmpty()) {
                        break;
                    }
                    continue;
                }

                if (comment.startsWith("#") || comment.startsWith("!")) {

                    preceding.add(
                            0,
                            comment.replaceFirst("^[#!]\\s?", "")
                    );

                } else {
                    break;
                }
            }

            for (String comment : preceding) {

                if (!comments.isEmpty()) {
                    comments.append(' ');
                }

                comments.append(comment);
            }

            if (!comments.isEmpty()) {
                break;
            }
        }

        String description = comments.toString().trim();

        if (description.isBlank()) {
            return null;
        }

        if (SECRET_PATTERN.matcher(key).find()) {
            return "Sensitive configuration value.";
        }

        return description;
    }

    private List<ConfigurationReference> extractReferences(
            Path file,
            String content,
            Path repositoryRoot,
            String fileType) {

        List<ConfigurationReference> references = new ArrayList<>();

        Path parent = file.toAbsolutePath().getParent();

        for (String line : content.split("\\R")) {

            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separator = findReferenceSeparator(trimmed);

            if (separator <= 0) {
                continue;
            }

            String key = trimmed.substring(0, separator).trim();

            if (!REFERENCE_KEYS.contains(key)) {
                continue;
            }

            String value = trimmed.substring(separator + 1).trim();

            if (value.isEmpty()) {
                continue;
            }

            String[] targets = value.split(",");

            for (String rawTarget : targets) {

                String target = rawTarget.trim()
                        .replaceAll("^['\"]|['\"]$", "");

                if (target.isEmpty()
                        || target.startsWith("${")
                        || target.startsWith("classpath:")
                        || target.startsWith("optional:")) {

                    references.add(
                            new ConfigurationReference(
                                    key,
                                    target,
                                    false
                            )
                    );

                    continue;
                }

                boolean resolved = false;

                try {

                    Path targetPath = Path.of(target);

                    Path resolvedPath = targetPath.isAbsolute()
                            ? targetPath
                            : parent.resolve(targetPath);

                    resolved = Files.exists(resolvedPath.normalize());

                    if (!resolved && repositoryRoot != null) {
                        resolved = Files.exists(
                                repositoryRoot.resolve(targetPath).normalize()
                        );
                    }

                } catch (RuntimeException ignored) {
                    // Invalid or unsupported reference path.
                }

                references.add(
                        new ConfigurationReference(
                                key,
                                target,
                                resolved
                        )
                );
            }
        }

        return List.copyOf(references);
    }

    private int findReferenceSeparator(String line) {

        int equals = line.indexOf('=');
        int colon = line.indexOf(':');

        if (equals < 0) {
            return colon;
        }

        if (colon < 0) {
            return equals;
        }

        return Math.min(equals, colon);
    }

    private ConfigurationFileAnalysis result(
            String filePath,
            String fileType,
            String environment,
            List<String> keys,
            List<ConfigurationKeyAnalysis> keyDetails,
            List<ConfigurationReference> references,
            String status,
            List<String> diagnostics) {

        return new ConfigurationFileAnalysis(
                filePath,
                fileType,
                keys,
                keyDetails,
                references,
                environment,
                status,
                diagnostics
        );
    }

    private ConfigurationFileAnalysis failure(
            String filePath,
            String fileType,
            String environment,
            String diagnostic) {

        return result(
                filePath,
                fileType,
                environment,
                List.of(),
                List.of(),
                List.of(),
                "FAILED",
                List.of(diagnostic)
        );
    }
}
