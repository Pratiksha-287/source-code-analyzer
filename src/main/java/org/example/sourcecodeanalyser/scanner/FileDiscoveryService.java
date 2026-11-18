
package org.example.sourcecodeanalyser.scanner;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileDiscoveryService {

    private static final Map<String, String> FILE_TYPES = createFileTypes();

    private static Map<String, String> createFileTypes() {

        Map<String, String> fileTypes = new LinkedHashMap<>();

        // Compound extensions first
        fileTypes.put(".gradle.kts", "GRADLE_KOTLIN");

        // Other extensions
        fileTypes.put(".java", "JAVA");
        fileTypes.put(".gradle", "GRADLE");
        fileTypes.put(".properties", "PROPERTIES");
        fileTypes.put(".xml", "XML");
        fileTypes.put(".yaml", "YAML");
        fileTypes.put(".yml", "YAML");
        fileTypes.put(".json", "JSON");
        fileTypes.put(".js", "JAVASCRIPT");
        fileTypes.put(".ts", "TYPESCRIPT");
        fileTypes.put(".tsx", "TYPESCRIPT");
        fileTypes.put(".jsx", "JAVASCRIPT");
        fileTypes.put(".html", "HTML");
        fileTypes.put(".css", "CSS");

        return java.util.Collections.unmodifiableMap(fileTypes);
    }

    public Map<String, List<Path>> classifyFiles(List<Path> files) {

        Map<String, List<Path>> categorizedFiles = new LinkedHashMap<>();

        for (String fileType : FILE_TYPES.values()) {
            categorizedFiles.putIfAbsent(fileType, new ArrayList<>());
        }

        categorizedFiles.put("OTHER", new ArrayList<>());

        for (Path file : files) {

            String fileName = file.getFileName()
                    .toString()
                    .toLowerCase(Locale.ROOT);

            String detectedType = "OTHER";

            for (Map.Entry<String, String> entry : FILE_TYPES.entrySet()) {

                if (fileName.endsWith(entry.getKey())) {
                    detectedType = entry.getValue();
                    break;
                }
            }

            categorizedFiles.get(detectedType).add(file);
        }

        return categorizedFiles;
    }
}
