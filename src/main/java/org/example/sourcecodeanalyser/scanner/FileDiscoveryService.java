package org.example.sourcecodeanalyser.scanner;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FileDiscoveryService {


    private static final Map<String, String> FILE_TYPES = Map.ofEntries(
            Map.entry(".java", "JAVA"),
            Map.entry(".gradle", "GRADLE"),
            Map.entry(".gradle.kts", "GRADLE_KOTLIN"),
            Map.entry(".properties", "PROPERTIES"),
            Map.entry(".xml", "XML"),
            Map.entry(".yaml", "YAML"),
            Map.entry(".yml", "YAML"),
            Map.entry(".json", "JSON"),
            Map.entry(".js", "JAVASCRIPT"),
            Map.entry(".ts", "TYPESCRIPT"),
            Map.entry(".tsx", "TYPESCRIPT"),
            Map.entry(".jsx", "JAVASCRIPT"),
            Map.entry(".html", "HTML"),
            Map.entry(".css", "CSS")
    );

    public Map<String, List<Path>> classifyFiles(List<Path> files) {

        Map<String, List<Path>> categorizedFiles =
                new LinkedHashMap<>();

        for (String fileType : Set.copyOf(FILE_TYPES.values())) {
            categorizedFiles.put(fileType, new ArrayList<>());
        }

        categorizedFiles.put("OTHER", new ArrayList<>());

        for (Path file : files) {

            String fileName = file.getFileName()
                    .toString()
                    .toLowerCase(Locale.ROOT);

            String detectedType = FILE_TYPES.entrySet()
                    .stream()
                    .filter(entry -> fileName.endsWith(entry.getKey()))
                    .max((first, second) ->
                            Integer.compare(
                                    first.getKey().length(),
                                    second.getKey().length()
                            ))
                    .map(Map.Entry::getValue)
                    .orElse("OTHER");

            categorizedFiles.get(detectedType).add(file);
        }

        return categorizedFiles;
    }
}
