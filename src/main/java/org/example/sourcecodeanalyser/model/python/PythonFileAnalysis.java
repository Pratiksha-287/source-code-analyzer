
package org.example.sourcecodeanalyser.model.python;

import java.util.List;

public record PythonFileAnalysis(
        String filePath,
        String moduleName,
        int lineCount,
        List<PythonImportInfo> imports,
        List<PythonExportInfo> exports,
        List<PythonClassInfo> classes,
        List<PythonFunctionInfo> functions,
        List<PythonVariableInfo> variables,
        List<PythonCallInfo> calls,
        List<PythonRelationshipInfo> relationships,
        List<PythonControlFlowInfo> controlFlow,
        List<PythonDiagnostic> diagnostics,
        List<PythonLambdaInfo> lambdas,
        List<PythonComprehensionInfo> comprehensions,
        List<PythonDocstringInfo> docstrings,
        List<PythonEnvironmentVariableInfo> environmentVariables,
        List<PythonOutboundHttpCallInfo> outboundHttpCalls,
        List<PythonRaisedExceptionInfo> raisedExceptions
) {
    public PythonFileAnalysis {
        imports = safeList(imports);
        exports = safeList(exports);
        classes = safeList(classes);
        functions = safeList(functions);
        variables = safeList(variables);
        calls = safeList(calls);
        relationships = safeList(relationships);
        controlFlow = safeList(controlFlow);
        diagnostics = safeList(diagnostics);
        lambdas = safeList(lambdas);
        comprehensions = safeList(comprehensions);
        docstrings = safeList(docstrings);
        environmentVariables = safeList(environmentVariables);
        outboundHttpCalls = safeList(outboundHttpCalls);
        raisedExceptions = safeList(raisedExceptions);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record SourceLocation(
            int startLine,
            int endLine

    ) {}

    public record PythonImportInfo(
            String moduleName,
            String importedName,
            String alias,
            boolean relativeImport,
            int relativeLevel,
            SourceLocation location
    ) {}

    public record PythonExportInfo(
            String name,
            String kind,
            boolean explicitlyExported,
            SourceLocation location
    ) {}

    public record PythonClassInfo(
            String name,
            List<String> decorators,
            List<String> baseClasses,
            List<PythonFunctionInfo> methods,
            List<PythonVariableInfo> attributes,
            SourceLocation location
    ) {
        public PythonClassInfo {
            decorators = safeList(decorators);
            baseClasses = safeList(baseClasses);
            methods = safeList(methods);
            attributes = safeList(attributes);
        }
    }

    public record PythonFunctionInfo(
            String name,
            List<String> decorators,
            List<PythonParameterInfo> parameters,
            String returnAnnotation,
            boolean asynchronous,
            boolean method,
            SourceLocation location,
            List<PythonFunctionInfo> nestedFunctions
    ) {
        public PythonFunctionInfo {
            decorators = safeList(decorators);
            parameters = safeList(parameters);
            nestedFunctions = safeList(nestedFunctions);
        }

        public PythonFunctionInfo(
                String name,
                List<String> decorators,
                List<PythonParameterInfo> parameters,
                String returnAnnotation,
                boolean asynchronous,
                boolean method,
                SourceLocation location
        ) {
            this(name, decorators, parameters, returnAnnotation,
                    asynchronous, method, location, List.of());
        }
    }

    public record PythonParameterInfo(
            String name,
            String parameterType,
            String defaultValue,
            String annotation,
            SourceLocation location
    ) {}

    public record PythonComprehensionInfo(
            String kind,
            String expression,
            String iterationClause,
            SourceLocation location
    ) {}

    public record PythonLambdaInfo(
            List<PythonParameterInfo> parameters,
            String expression,
            SourceLocation location
    ) {
        public PythonLambdaInfo {
            parameters = safeList(parameters);
        }
    }

    public record PythonVariableInfo(
            String name,
            String value,
            String scope,
            String annotation,
            SourceLocation location
    ) {}

    public record PythonCallInfo(
            String calledFunction,
            String receiver,
            List<String> arguments,
            SourceLocation location
    ) {
        public PythonCallInfo {
            arguments = safeList(arguments);
        }
    }

    public record PythonRelationshipInfo(
            String source,
            String target,
            String relationshipType,
            SourceLocation location
    ) {}

    public record PythonControlFlowInfo(
            String constructType,
            String condition,
            SourceLocation location
    ) {}

    public record PythonDiagnostic(
            String message,
            int line,
            int column,
            String severity
    ) {}

    public record PythonDocstringInfo(
            String ownerType,
            String ownerName,
            String content,
            SourceLocation location
    ) {}

    public record PythonEnvironmentVariableInfo(
            String name,
            String accessType,
            String expression,
            String scope,
            SourceLocation location
    ) {}

    public record PythonOutboundHttpCallInfo(
            String library,
            String method,
            String urlExpression,
            List<String> arguments,
            SourceLocation location
    ) {
        public PythonOutboundHttpCallInfo {
            arguments = safeList(arguments);
        }
    }

    public record PythonRaisedExceptionInfo(
            String exceptionType,
            String expression,
            String causeExpression,
            boolean reRaise,
            SourceLocation location
    ) {}
}
