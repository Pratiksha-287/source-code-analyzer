
package org.example.sourcecodeanalyser.frontend.model;

import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;

import java.nio.file.Path;

public record FrontendFileParseResult(
        Path filePath,
        Swc4jParseOutput parseOutput,
        String errorMessage
) {

    public boolean isSuccessful() {
        return parseOutput != null;
    }
}
