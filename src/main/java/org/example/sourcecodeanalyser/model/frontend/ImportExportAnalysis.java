
package org.example.sourcecodeanalyser.model.frontend;

import java.util.List;

public record ImportExportAnalysis(
        List<ImportInfo> imports,
        List<ExportInfo> exports
) {

    public record ImportInfo(
            String source,
            List<String> specifiers,
            boolean typeOnly,
            List<ImportSpecifierInfo> structuredSpecifiers
    ) {
        // Backward-compatible constructor.
        public ImportInfo(
                String source,
                List<String> specifiers,
                boolean typeOnly
        ) {
            this(source, specifiers, typeOnly, List.of());
        }
    }

    public record ImportSpecifierInfo(
            String kind,
            String importedName,
            String localName
    ) {}

    public record ExportInfo(
            String kind,
            String source,
            List<String> names,
            boolean typeOnly
    ) {}
}
