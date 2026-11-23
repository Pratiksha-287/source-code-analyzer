
package org.example.sourcecodeanalyser.model.clientextension;

import org.example.sourcecodeanalyser.model.frontend.FunctionAnalysis;
import org.example.sourcecodeanalyser.model.frontend.ImportExportAnalysis;
import org.example.sourcecodeanalyser.model.frontend.ReactAnalysis;

public record FrontendFileAnalysis(
        String filePath,
        boolean successful,
        String errorMessage,
        ImportExportAnalysis importExportAnalysis,
        FunctionAnalysis functionAnalysis,
        ReactAnalysis reactAnalysis
) {
}
