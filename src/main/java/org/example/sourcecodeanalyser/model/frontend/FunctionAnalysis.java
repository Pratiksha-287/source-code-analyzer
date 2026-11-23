package org.example.sourcecodeanalyser.model.frontend;

import java.util.List;

public record FunctionAnalysis(
        List<FunctionInfo> functions
) {
    public record FunctionInfo(
            String name,
            String kind,
            List<String> parameters,
            boolean async
    ) {}
}