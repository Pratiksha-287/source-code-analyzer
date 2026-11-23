package org.example.sourcecodeanalyser.analyzer.python;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.example.sourcecodeanalyser.parser.python.Python3Parser;
import org.example.sourcecodeanalyser.parser.python.Python3ParserBaseVisitor;
import org.example.sourcecodeanalyser.parser.python.PythonAstTextExtractor;
import org.example.sourcecodeanalyser.parser.python.PythonSourceParser;

import org.example.sourcecodeanalyser.model.python.PythonFileAnalysis;
import org.example.sourcecodeanalyser.model.python.PythonFileAnalysis.*;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PythonFileAnalyzer {

    private final PythonSourceParser sourceParser = new PythonSourceParser();

    public PythonFileAnalysis analyze(Path filePath) throws IOException {

        PythonSourceParser.ParseResult parseResult = sourceParser.parse(filePath);

        AnalysisVisitor visitor = new AnalysisVisitor();

        // Module docstring must be the first statement in the module.
        visitor.extractAndAddDocstring(
                parseResult.parseTree(),
                "MODULE",
                filePath.getFileName().toString()
        );

        visitor.visit(parseResult.parseTree());
        visitor.buildExports();

        List<PythonDiagnostic> diagnostics = parseResult.errors()
                .stream()
                .map(error -> new PythonDiagnostic(
                        error.message(),
                        error.line(),
                        error.column(),
                        "ERROR"
                ))
                .toList();

        String fileName = filePath.getFileName().toString();

        String moduleName = fileName.endsWith(".py")
                ? fileName.substring(0, fileName.length() - 3)
                : fileName;

        return new PythonFileAnalysis(
                filePath.toString(),
                moduleName,
                Files.readAllLines(filePath).size(),
                visitor.imports,
                visitor.exports,
                visitor.classes,
                visitor.functions,
                visitor.variables,
                visitor.calls,
                visitor.relationships,
                visitor.controlFlow,
                diagnostics,
                visitor.lambdas,
                visitor.comprehensions,
                visitor.docstrings,
                visitor.environmentVariables,
                visitor.outboundHttpCalls,
                visitor.raisedExceptions
        );
    }

    private static class AnalysisVisitor
            extends Python3ParserBaseVisitor<Void> {

        private final List<PythonImportInfo> imports = new ArrayList<>();
        private final List<PythonExportInfo> exports = new ArrayList<>();
        private final List<PythonClassInfo> classes = new ArrayList<>();
        private final List<PythonFunctionInfo> functions = new ArrayList<>();
        private final List<PythonVariableInfo> variables = new ArrayList<>();
        private final List<PythonCallInfo> calls = new ArrayList<>();
        private final List<PythonRelationshipInfo> relationships = new ArrayList<>();
        private final List<PythonControlFlowInfo> controlFlow = new ArrayList<>();
        private final List<PythonLambdaInfo> lambdas = new ArrayList<>();
        private final List<PythonComprehensionInfo> comprehensions = new ArrayList<>();

        // New extraction results.
        private final List<PythonDocstringInfo> docstrings = new ArrayList<>();
        private final List<PythonEnvironmentVariableInfo> environmentVariables =
                new ArrayList<>();
        private final List<PythonOutboundHttpCallInfo> outboundHttpCalls =
                new ArrayList<>();
        private final List<PythonRaisedExceptionInfo> raisedExceptions =
                new ArrayList<>();

        private final List<ClassBuilder> classStack = new ArrayList<>();

        private List<String> pendingDecorators = new ArrayList<>();

        private final Map<String, PythonExportInfo> moduleSymbols =
                new LinkedHashMap<>();

        private final Set<String> explicitlyExportedNames =
                new LinkedHashSet<>();

        /*
         * Assignment targets are tracked while visiting an assignment.
         * This prevents os.environ["KEY"] = value from being reported
         * as an environment-variable read.
         */
        private final Set<String> environmentWriteExpressions =
                new LinkedHashSet<>();

        private boolean hasExplicitAll = false;
        private SourceLocation allLocation;

        private int functionDepth = 0;

        private final List<String> functionStack = new ArrayList<>();

        private final List<FunctionBuilder> functionBuilderStack =
                new ArrayList<>();

        // ---------------------------------------------------------
        // MODULE SYMBOL AND EXPORT ANALYSIS
        // ---------------------------------------------------------

        private void registerModuleSymbol(
                String name,
                String kind,
                SourceLocation sourceLocation
        ) {

            if (name == null || name.isBlank()) {
                return;
            }

            moduleSymbols.put(
                    name,
                    new PythonExportInfo(
                            name,
                            kind,
                            false,
                            sourceLocation
                    )
            );
        }

        private void buildExports() {

            exports.clear();

            if (hasExplicitAll) {

                for (String name : explicitlyExportedNames) {

                    PythonExportInfo symbol = moduleSymbols.get(name);

                    String kind = symbol == null
                            ? "UNKNOWN"
                            : symbol.kind();

                    exports.add(new PythonExportInfo(
                            name,
                            kind,
                            true,
                            symbol == null
                                    ? allLocation
                                    : symbol.location()
                    ));
                }

                return;
            }

            for (PythonExportInfo symbol : moduleSymbols.values()) {

                if (!symbol.name().startsWith("_")) {
                    exports.add(symbol);
                }
            }
        }

        // ---------------------------------------------------------
        // DOCSTRING ANALYSIS
        // ---------------------------------------------------------

        private void extractAndAddDocstring(
                ParseTree container,
                String ownerType,
                String ownerName
        ) {

            ParseTree firstSimpleStatement =
                    findFirstTopLevelSimpleStatement(container);

            if (firstSimpleStatement == null) {
                return;
            }

            String statementText =
                    PythonAstTextExtractor.getText(firstSimpleStatement).trim();

            String content = extractStringLiteralDocstring(statementText);

            if (content == null) {
                return;
            }

            SourceLocation sourceLocation =
                    firstSimpleStatement instanceof ParserRuleContext context
                            ? location(context)
                            : null;

            docstrings.add(new PythonDocstringInfo(
                    ownerType,
                    ownerName,
                    content,
                    sourceLocation
            ));
        }

        /*
         * Finds only the first statement belonging directly to the supplied
         * module or block. It deliberately does not descend into nested
         * if/for/function/class bodies.
         */
        private ParseTree findFirstTopLevelSimpleStatement(
                ParseTree container
        ) {

            if (container == null) {
                return null;
            }

            for (int i = 0; i < container.getChildCount(); i++) {

                ParseTree child = container.getChild(i);

                if (!(child instanceof ParserRuleContext)) {
                    continue;
                }

                String ruleName =
                        child.getClass().getSimpleName();

                if ("Simple_stmtsContext".equals(ruleName)) {

                    for (int j = 0; j < child.getChildCount(); j++) {

                        ParseTree statement = child.getChild(j);

                        if ("Simple_stmtContext".equals(
                                statement.getClass().getSimpleName())) {

                            return statement;
                        }
                    }

                    return null;
                }

                if ("StmtContext".equals(ruleName)) {

                    for (int j = 0; j < child.getChildCount(); j++) {

                        ParseTree statementChild = child.getChild(j);

                        if ("Simple_stmtsContext".equals(
                                statementChild.getClass().getSimpleName())) {

                            for (int k = 0;
                                 k < statementChild.getChildCount();
                                 k++) {

                                ParseTree simpleStatement =
                                        statementChild.getChild(k);

                                if ("Simple_stmtContext".equals(
                                        simpleStatement.getClass()
                                                .getSimpleName())) {

                                    return simpleStatement;
                                }
                            }
                        }
                    }

                    // The first statement is compound, not a docstring.
                    return null;
                }
            }

            return null;
        }

        /*
         * Python docstrings are string literal expressions.
         * Bytes literals and f-strings are intentionally excluded.
         *
         * The returned content is the literal's source content, without
         * its surrounding quote characters. Escape sequences are retained
         * as written in the source.
         */
        private String extractStringLiteralDocstring(String statement) {

            if (statement == null || statement.isBlank()) {
                return null;
            }

            String text = statement.trim();

            Pattern pattern = Pattern.compile(
                    "^(?i: r|u|ur|ru)?\\s*"
                            .replace(" ", "")
                            + "(\"\"\"|'''|\"|')([\\s\\S]*)\\1$"
            );

            Matcher matcher = pattern.matcher(text);

            if (!matcher.matches()) {
                return null;
            }

            String prefixAndQuotes = text.substring(
                    0,
                    text.indexOf(matcher.group(1))
            ).toLowerCase();

            if (prefixAndQuotes.contains("f")
                    || prefixAndQuotes.contains("b")) {
                return null;
            }

            return matcher.group(2);
        }

        // ---------------------------------------------------------
        // DECORATORS
        // ---------------------------------------------------------

        @Override
        public Void visitDecorated(Python3Parser.DecoratedContext ctx) {

            List<String> decorators = ctx.decorators()
                    .decorator()
                    .stream()
                    .map(PythonAstTextExtractor::getText)
                    .map(String::trim)
                    .toList();

            List<String> previousDecorators = pendingDecorators;
            pendingDecorators = decorators;

            try {

                if (ctx.classdef() != null) {
                    visit(ctx.classdef());

                } else if (ctx.funcdef() != null) {
                    visit(ctx.funcdef());

                } else if (ctx.async_funcdef() != null) {
                    visit(ctx.async_funcdef());
                }

            } finally {
                pendingDecorators = previousDecorators;
            }

            return null;
        }

        private List<String> consumePendingDecorators() {

            List<String> decorators = pendingDecorators;
            pendingDecorators = new ArrayList<>();

            return decorators;
        }

        // ---------------------------------------------------------
        // CLASS ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitClassdef(Python3Parser.ClassdefContext ctx) {

            String className = ctx.name().getText();

            if (classStack.isEmpty() && functionDepth == 0) {

                registerModuleSymbol(
                        className,
                        "CLASS",
                        location(ctx)
                );
            }

            List<String> decorators = consumePendingDecorators();

            List<String> baseClasses = new ArrayList<>();

            if (ctx.arglist() != null) {

                ctx.arglist().argument().forEach(argument ->
                        baseClasses.add(
                                PythonAstTextExtractor.getText(argument)
                        )
                );
            }

            ClassBuilder builder = new ClassBuilder(
                    className,
                    decorators,
                    baseClasses,
                    location(ctx)
            );

            // A class docstring is the first statement in its class block.
            extractAndAddDocstring(
                    ctx.block(),
                    "CLASS",
                    className
            );

            classStack.add(builder);

            try {
                visit(ctx.block());
            } finally {
                classStack.remove(classStack.size() - 1);
            }

            classes.add(builder.build());

            for (String baseClass : baseClasses) {

                relationships.add(new PythonRelationshipInfo(
                        className,
                        baseClass,
                        "INHERITANCE",
                        location(ctx)
                ));
            }

            return null;
        }

        // ---------------------------------------------------------
        // FUNCTION AND METHOD ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitFuncdef(Python3Parser.FuncdefContext ctx) {

            analyzeFunction(ctx, false);

            return null;
        }

        @Override
        public Void visitAsync_funcdef(
                Python3Parser.Async_funcdefContext ctx
        ) {

            analyzeFunction(ctx.funcdef(), true);

            return null;
        }

        @Override
        public Void visitAsync_stmt(
                Python3Parser.Async_stmtContext ctx
        ) {

            if (ctx.funcdef() != null) {
                analyzeFunction(ctx.funcdef(), true);
                return null;
            }

            return visitChildren(ctx);
        }

        private void analyzeFunction(
                Python3Parser.FuncdefContext ctx,
                boolean asynchronous
        ) {

            String functionName = ctx.name().getText();

            if (classStack.isEmpty() && functionDepth == 0) {

                registerModuleSymbol(
                        functionName,
                        "FUNCTION",
                        location(ctx)
                );
            }

            List<String> decorators = consumePendingDecorators();

            List<PythonParameterInfo> parameters =
                    extractParameters(ctx.parameters());

            String returnAnnotation = ctx.test() == null
                    ? null
                    : PythonAstTextExtractor.getText(ctx.test());

            FunctionBuilder parent = functionBuilderStack.isEmpty()
                    ? null
                    : functionBuilderStack.get(
                    functionBuilderStack.size() - 1
            );

            boolean nestedFunction = parent != null
                    && parent.classDepth == classStack.size();

            boolean method = !classStack.isEmpty()
                    && !nestedFunction;

            FunctionBuilder builder = new FunctionBuilder(
                    functionName,
                    decorators,
                    parameters,
                    returnAnnotation,
                    asynchronous,
                    method,
                    location(ctx),
                    classStack.size()
            );

            if (nestedFunction) {
                parent.nestedFunctions.add(builder);
            }

            String callerName;

            if (nestedFunction) {

                callerName = parent.qualifiedName + "." + functionName;

            } else if (method) {

                String className = classStack
                        .get(classStack.size() - 1)
                        .name;

                callerName = className + "." + functionName;

            } else {

                callerName = functionName;
            }

            builder.qualifiedName = callerName;

            // A function docstring is its first statement.
            extractAndAddDocstring(
                    ctx.block(),
                    "FUNCTION",
                    callerName
            );

            functionBuilderStack.add(builder);
            functionStack.add(callerName);
            functionDepth++;

            try {
                visit(ctx.block());
            } finally {

                functionDepth--;

                functionStack.remove(functionStack.size() - 1);

                functionBuilderStack.remove(
                        functionBuilderStack.size() - 1
                );
            }

            if (!nestedFunction) {

                PythonFunctionInfo functionInfo = builder.build();

                if (method) {

                    classStack.get(classStack.size() - 1)
                            .methods.add(functionInfo);

                } else {

                    functions.add(functionInfo);
                }
            }
        }

        // ---------------------------------------------------------
        // FUNCTION PARAMETER ANALYSIS
        // ---------------------------------------------------------

        private List<PythonParameterInfo> extractParameters(
                Python3Parser.ParametersContext ctx
        ) {

            List<PythonParameterInfo> parameters = new ArrayList<>();

            if (ctx == null || ctx.typedargslist() == null) {
                return parameters;
            }

            Python3Parser.TypedargslistContext typedArgs =
                    ctx.typedargslist();

            List<ParseTree> children = typedArgs.children;

            if (children == null) {
                return parameters;
            }

            String nextParameterType = "POSITIONAL_OR_KEYWORD";

            boolean keywordOnlyMode = false;
            boolean expectingDefault = false;

            List<ParameterBuilder> builders = new ArrayList<>();

            for (int i = 0; i < children.size(); i++) {

                ParseTree child = children.get(i);

                if (child instanceof Python3Parser.TfpdefContext parameter) {

                    String parameterType = nextParameterType;

                    if ("POSITIONAL_OR_KEYWORD".equals(parameterType)
                            && keywordOnlyMode) {

                        parameterType = "KEYWORD_ONLY";
                    }

                    String annotation = parameter.test() == null
                            ? null
                            : PythonAstTextExtractor.getText(
                            parameter.test()
                    );

                    ParameterBuilder builder = new ParameterBuilder(
                            parameter.name().getText(),
                            parameterType,
                            annotation,
                            location(parameter)
                    );

                    builders.add(builder);

                    nextParameterType = keywordOnlyMode
                            ? "KEYWORD_ONLY"
                            : "POSITIONAL_OR_KEYWORD";

                    expectingDefault = false;

                    if ("VAR_POSITIONAL".equals(parameterType)) {

                        keywordOnlyMode = true;
                        nextParameterType = "KEYWORD_ONLY";
                    }

                    continue;
                }

                if (child instanceof Python3Parser.TestContext test
                        && expectingDefault
                        && !builders.isEmpty()) {

                    builders.get(builders.size() - 1).defaultValue =
                            PythonAstTextExtractor.getText(test);

                    expectingDefault = false;

                    continue;
                }

                if (child instanceof TerminalNode terminal) {

                    int tokenType = terminal.getSymbol().getType();

                    if (tokenType == Python3Parser.ASSIGN) {

                        expectingDefault = true;

                    } else if (tokenType == Python3Parser.STAR) {

                        boolean hasNamedVararg =
                                i + 1 < children.size()
                                        && children.get(i + 1)
                                        instanceof Python3Parser.TfpdefContext;

                        if (hasNamedVararg) {

                            nextParameterType = "VAR_POSITIONAL";

                        } else {

                            keywordOnlyMode = true;
                            nextParameterType = "KEYWORD_ONLY";
                        }

                    } else if (tokenType == Python3Parser.POWER) {

                        nextParameterType = "VAR_KEYWORD";
                    }
                }
            }

            for (ParameterBuilder builder : builders) {

                parameters.add(new PythonParameterInfo(
                        builder.name,
                        builder.parameterType,
                        builder.defaultValue,
                        builder.annotation,
                        builder.location
                ));
            }

            return parameters;
        }

        private static class ParameterBuilder {

            private final String name;
            private final String parameterType;
            private final String annotation;
            private final SourceLocation location;

            private String defaultValue;

            private ParameterBuilder(
                    String name,
                    String parameterType,
                    String annotation,
                    SourceLocation location
            ) {

                this.name = name;
                this.parameterType = parameterType;
                this.annotation = annotation;
                this.location = location;
            }
        }

        // ---------------------------------------------------------
        // LAMBDA ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitLambdef(Python3Parser.LambdefContext ctx) {

            lambdas.add(new PythonLambdaInfo(
                    extractLambdaParameters(ctx.varargslist()),
                    PythonAstTextExtractor.getText(ctx.test()),
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        @Override
        public Void visitLambdef_nocond(
                Python3Parser.Lambdef_nocondContext ctx
        ) {

            lambdas.add(new PythonLambdaInfo(
                    extractLambdaParameters(ctx.varargslist()),
                    PythonAstTextExtractor.getText(ctx.test_nocond()),
                    location(ctx)
            ));

            return visitChildren(ctx);
        }



        private List<PythonParameterInfo> extractLambdaParameters(
                Python3Parser.VarargslistContext ctx
        ) {
            List<PythonParameterInfo> parameters = new ArrayList<>();

            if (ctx == null || ctx.children == null) {
                return parameters;
            }

            List<ParseTree> children = ctx.children;

            String nextParameterType = "POSITIONAL_OR_KEYWORD";
            boolean keywordOnlyMode = false;
            boolean expectingDefault = false;

            List<ParameterBuilder> builders = new ArrayList<>();

            for (int i = 0; i < children.size(); i++) {

                ParseTree child = children.get(i);

                if (child instanceof Python3Parser.VfpdefContext parameter) {

                    String parameterType = nextParameterType;

                    if ("POSITIONAL_OR_KEYWORD".equals(parameterType)
                            && keywordOnlyMode) {
                        parameterType = "KEYWORD_ONLY";
                    }

                    ParameterBuilder builder = new ParameterBuilder(
                            parameter.name().getText(),
                            parameterType,
                            null,
                            location(parameter)
                    );

                    builders.add(builder);

                    nextParameterType = keywordOnlyMode
                            ? "KEYWORD_ONLY"
                            : "POSITIONAL_OR_KEYWORD";

                    expectingDefault = false;

                    if ("VAR_POSITIONAL".equals(parameterType)) {
                        keywordOnlyMode = true;
                        nextParameterType = "KEYWORD_ONLY";
                    }

                    continue;
                }

                if (child instanceof Python3Parser.TestContext test
                        && expectingDefault
                        && !builders.isEmpty()) {

                    builders.get(builders.size() - 1).defaultValue =
                            PythonAstTextExtractor.getText(test);

                    expectingDefault = false;
                    continue;
                }

                if (child instanceof TerminalNode terminal) {

                    int tokenType = terminal.getSymbol().getType();

                    if (tokenType == Python3Parser.ASSIGN) {

                        expectingDefault = true;

                    } else if (tokenType == Python3Parser.STAR) {

                        boolean hasNamedVararg =
                                i + 1 < children.size()
                                        && children.get(i + 1)
                                        instanceof Python3Parser.VfpdefContext;

                        if (hasNamedVararg) {
                            nextParameterType = "VAR_POSITIONAL";
                        } else {
                            keywordOnlyMode = true;
                            nextParameterType = "KEYWORD_ONLY";
                        }

                    } else if (tokenType == Python3Parser.POWER) {

                        nextParameterType = "VAR_KEYWORD";
                    }
                }
            }

            for (ParameterBuilder builder : builders) {

                parameters.add(new PythonParameterInfo(
                        builder.name,
                        builder.parameterType,
                        builder.defaultValue,
                        builder.annotation,
                        builder.location
                ));
            }

            return parameters;
        }



        // ---------------------------------------------------------
        // COMPREHENSION AND GENERATOR EXPRESSION ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitAtom(Python3Parser.AtomContext ctx) {

            if (ctx.OPEN_PAREN() != null
                    && ctx.testlist_comp() != null
                    && ctx.testlist_comp().comp_for() != null) {

                String completeText =
                        PythonAstTextExtractor.getText(ctx.testlist_comp());

                String iterationClause =
                        PythonAstTextExtractor.getText(
                                ctx.testlist_comp().comp_for()
                        );

                addComprehension(
                        "GENERATOR_EXPRESSION",
                        completeText,
                        iterationClause,
                        location(ctx)
                );
            }

            if (ctx.OPEN_BRACK() != null
                    && ctx.testlist_comp() != null
                    && ctx.testlist_comp().comp_for() != null) {

                String completeText =
                        PythonAstTextExtractor.getText(ctx.testlist_comp());

                String iterationClause =
                        PythonAstTextExtractor.getText(
                                ctx.testlist_comp().comp_for()
                        );

                addComprehension(
                        "LIST_COMPREHENSION",
                        completeText,
                        iterationClause,
                        location(ctx)
                );
            }

            if (ctx.OPEN_BRACE() != null
                    && ctx.dictorsetmaker() != null
                    && ctx.dictorsetmaker().comp_for() != null) {

                Python3Parser.DictorsetmakerContext maker =
                        ctx.dictorsetmaker();

                String completeText =
                        PythonAstTextExtractor.getText(maker);

                String iterationClause =
                        PythonAstTextExtractor.getText(maker.comp_for());

                String kind = maker.COLON().isEmpty()
                        ? "SET_COMPREHENSION"
                        : "DICTIONARY_COMPREHENSION";

                addComprehension(
                        kind,
                        completeText,
                        iterationClause,
                        location(ctx)
                );
            }

            return visitChildren(ctx);
        }

        private void addComprehension(
                String kind,
                String expression,
                String iterationClause,
                SourceLocation sourceLocation
        ) {

            comprehensions.add(new PythonComprehensionInfo(
                    kind,
                    expression,
                    iterationClause,
                    sourceLocation
            ));
        }

        // ---------------------------------------------------------
        // IMPORT ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitImport_name(
                Python3Parser.Import_nameContext ctx
        ) {

            for (Python3Parser.Dotted_as_nameContext item :
                    ctx.dotted_as_names().dotted_as_name()) {

                String moduleName = item.dotted_name().getText();

                String alias = item.name() == null
                        ? null
                        : item.name().getText();

                String exportedName = alias != null
                        ? alias
                        : moduleName.split("\\.")[0];

                if (classStack.isEmpty() && functionDepth == 0) {

                    registerModuleSymbol(
                            exportedName,
                            "IMPORT",
                            location(item)
                    );
                }

                imports.add(new PythonImportInfo(
                        moduleName,
                        null,
                        alias,
                        false,
                        0,
                        location(item)
                ));
            }

            return null;
        }

        @Override
        public Void visitImport_from(
                Python3Parser.Import_fromContext ctx
        ) {

            int relativeLevel =
                    ctx.DOT().size() + (ctx.ELLIPSIS().size() * 3);

            boolean relativeImport = relativeLevel > 0;

            String moduleName = ctx.dotted_name() == null
                    ? ""
                    : ctx.dotted_name().getText();

            if (ctx.STAR() != null) {

                imports.add(new PythonImportInfo(
                        moduleName,
                        "*",
                        null,
                        relativeImport,
                        relativeLevel,
                        location(ctx)
                ));

                return null;
            }

            if (ctx.import_as_names() != null) {

                for (Python3Parser.Import_as_nameContext item :
                        ctx.import_as_names().import_as_name()) {

                    List<Python3Parser.NameContext> names = item.name();

                    String importedName = names.get(0).getText();

                    String alias = names.size() > 1
                            ? names.get(1).getText()
                            : null;

                    String exportedName = alias != null
                            ? alias
                            : importedName;

                    if (classStack.isEmpty() && functionDepth == 0) {

                        registerModuleSymbol(
                                exportedName,
                                "IMPORT",
                                location(item)
                        );
                    }

                    imports.add(new PythonImportInfo(
                            moduleName,
                            importedName,
                            alias,
                            relativeImport,
                            relativeLevel,
                            location(item)
                    ));
                }
            }

            return null;
        }

        // ---------------------------------------------------------
        // VARIABLE AND ASSIGNMENT ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitExpr_stmt(
                Python3Parser.Expr_stmtContext ctx
        ) {

            List<Python3Parser.Testlist_star_exprContext> expressions =
                    ctx.testlist_star_expr();

            boolean isAssignment =
                    !ctx.ASSIGN().isEmpty()
                            || ctx.annassign() != null
                            || ctx.augassign() != null;

            if (isAssignment && !expressions.isEmpty()) {

                String target =
                        PythonAstTextExtractor.getText(expressions.get(0));

                String value = null;
                String annotation = null;

                if (expressions.size() > 1) {

                    value = PythonAstTextExtractor.getText(
                            expressions.get(expressions.size() - 1)
                    );
                }

                if (ctx.annassign() != null) {

                    List<Python3Parser.TestContext> annotationTests =
                            ctx.annassign().test();

                    if (!annotationTests.isEmpty()) {

                        annotation = PythonAstTextExtractor.getText(
                                annotationTests.get(0)
                        );
                    }

                    if (ctx.annassign().ASSIGN() == null) {

                        value = null;

                    } else if (annotationTests.size() > 1) {

                        value = PythonAstTextExtractor.getText(
                                annotationTests.get(
                                        annotationTests.size() - 1
                                )
                        );
                    }
                }

                if ("__all__".equals(target)
                        && classStack.isEmpty()
                        && functionDepth == 0
                        && value != null) {

                    hasExplicitAll = true;
                    allLocation = location(ctx);

                    explicitlyExportedNames.clear();

                    Matcher matcher = Pattern.compile(
                            "['\"]([^'\"]+)['\"]"
                    ).matcher(value);

                    while (matcher.find()) {

                        explicitlyExportedNames.add(
                                matcher.group(1)
                        );
                    }
                }

                recordVariable(
                        target,
                        value,
                        annotation,
                        location(ctx)
                );

                /*
                 * Remember environment subscription assignment targets
                 * during child traversal, so they are not misclassified
                 * as reads.
                 */
                boolean environmentWrite =
                        target.startsWith("os.environ[");

                if (environmentWrite) {
                    environmentWriteExpressions.add(target);
                }

                try {
                    return visitChildren(ctx);
                } finally {
                    if (environmentWrite) {
                        environmentWriteExpressions.remove(target);
                    }
                }
            }

            return visitChildren(ctx);
        }

        private void recordVariable(
                String target,
                String value,
                String annotation,
                SourceLocation sourceLocation
        ) {

            if (target == null || target.isBlank()) {
                return;
            }

            String scope;

            if (!classStack.isEmpty() && functionDepth == 0) {

                scope = "CLASS";

            } else if (functionDepth > 0
                    && !classStack.isEmpty()
                    && target.startsWith("self.")) {

                scope = "INSTANCE_ATTRIBUTE";

            } else if (functionDepth > 0) {

                String qualifiedFunction = functionStack.isEmpty()
                        ? "<function>"
                        : functionStack.get(functionStack.size() - 1);

                scope = "FUNCTION:" + qualifiedFunction;

            } else {

                scope = "MODULE";
            }

            if ("MODULE".equals(scope)
                    && !"__all__".equals(target)) {

                registerModuleSymbol(
                        target,
                        "VARIABLE",
                        sourceLocation
                );
            }

            PythonVariableInfo variable = new PythonVariableInfo(
                    target,
                    value,
                    scope,
                    annotation,
                    sourceLocation
            );

            if ("CLASS".equals(scope)
                    || "INSTANCE_ATTRIBUTE".equals(scope)) {

                classStack.get(classStack.size() - 1)
                        .attributes.add(variable);

            } else {

                variables.add(variable);
            }
        }

        // ---------------------------------------------------------
        // CALL, ENVIRONMENT VARIABLE AND HTTP ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitAtom_expr(
                Python3Parser.Atom_exprContext ctx
        ) {

            StringBuilder expression = new StringBuilder(
                    PythonAstTextExtractor.getText(ctx.atom())
            );

            for (Python3Parser.TrailerContext trailer : ctx.trailer()) {

                if (trailer.DOT() != null) {

                    expression.append(".")
                            .append(trailer.name().getText());

                } else if (trailer.OPEN_BRACK() != null) {

                    String subscript = PythonAstTextExtractor.getText(
                            trailer.subscriptlist()
                    );

                    String completeSubscript =
                            expression + "[" + subscript + "]";

                    if (expression.toString().equals("os.environ")
                            && !environmentWriteExpressions.contains(
                            completeSubscript
                    )) {

                        addEnvironmentVariable(
                                subscript,
                                "OS_ENVIRON_SUBSCRIPT",
                                completeSubscript,
                                location(ctx)
                        );
                    }

                    expression.append("[")
                            .append(subscript)
                            .append("]");

                } else if (trailer.OPEN_PAREN() != null) {

                    String calledFunction = expression.toString();

                    String receiver = null;

                    int lastDot = calledFunction.lastIndexOf('.');

                    if (lastDot > 0) {

                        receiver = calledFunction.substring(
                                0,
                                lastDot
                        );
                    }

                    List<String> arguments = new ArrayList<>();

                    if (trailer.arglist() != null) {

                        trailer.arglist().argument().forEach(argument ->
                                arguments.add(
                                        PythonAstTextExtractor.getText(argument)
                                )
                        );
                    }

                    SourceLocation callLocation = location(ctx);

                    calls.add(new PythonCallInfo(
                            calledFunction,
                            receiver,
                            arguments,
                            callLocation
                    ));

                    analyzeEnvironmentCall(
                            calledFunction,
                            arguments,
                            callLocation
                    );

                    analyzeOutboundHttpCall(
                            calledFunction,
                            arguments,
                            callLocation
                    );

                    String caller = null;

                    if (!functionStack.isEmpty()) {

                        caller = functionStack.get(
                                functionStack.size() - 1
                        );

                    } else if (classStack.isEmpty()) {

                        caller = "<module>";
                    }

                    if (caller != null) {

                        relationships.add(new PythonRelationshipInfo(
                                caller,
                                calledFunction,
                                "FUNCTION_CALL",
                                callLocation
                        ));
                    }

                    expression.append("()");
                }
            }

            return visitChildren(ctx);
        }

        // ---------------------------------------------------------
        // ENVIRONMENT VARIABLE EXTRACTION
        // ---------------------------------------------------------

        private void analyzeEnvironmentCall(
                String calledFunction,
                List<String> arguments,
                SourceLocation sourceLocation
        ) {

            String accessType = null;

            if ("os.getenv".equals(calledFunction)) {

                accessType = "OS_GETENV";

            } else if ("os.environ.get".equals(calledFunction)) {

                accessType = "OS_ENVIRON_GET";
            }

            if (accessType == null || arguments.isEmpty()) {
                return;
            }

            String keyExpression = extractArgumentValue(arguments.get(0));

            addEnvironmentVariable(
                    keyExpression,
                    accessType,
                    calledFunction + "(" + String.join(", ", arguments) + ")",
                    sourceLocation
            );
        }

        private void addEnvironmentVariable(
                String keyExpression,
                String accessType,
                String expression,
                SourceLocation sourceLocation
        ) {

            if (keyExpression == null || keyExpression.isBlank()) {
                return;
            }

            String name = extractStringValue(keyExpression);

            if (name == null || name.isBlank()) {
                name = keyExpression.trim();
            }

            String scope;

            if (!functionStack.isEmpty()) {

                scope = "FUNCTION:" + functionStack.get(
                        functionStack.size() - 1
                );

            } else if (!classStack.isEmpty()) {

                scope = "CLASS:" + classStack.get(
                        classStack.size() - 1
                ).name;

            } else {

                scope = "MODULE";
            }

            environmentVariables.add(
                    new PythonEnvironmentVariableInfo(
                            name,
                            accessType,
                            expression,
                            scope,
                            sourceLocation
                    )
            );
        }

        // ---------------------------------------------------------
        // OUTBOUND HTTP CALL EXTRACTION
        // ---------------------------------------------------------

        private void analyzeOutboundHttpCall(
                String calledFunction,
                List<String> arguments,
                SourceLocation sourceLocation
        ) {

            String library;
            String method;

            if (calledFunction.startsWith("requests.")) {

                library = "requests";
                method = calledFunction.substring("requests.".length());

                Set<String> supportedMethods = Set.of(
                        "get",
                        "post",
                        "put",
                        "patch",
                        "delete",
                        "head",
                        "options",
                        "request"
                );

                if (!supportedMethods.contains(method)) {
                    return;
                }

            } else if ("urllib.request.urlopen".equals(calledFunction)) {

                library = "urllib";
                method = "urlopen";

            } else if ("urllib.request.Request".equals(calledFunction)) {

                library = "urllib";
                method = "Request";

            } else if ("urllib.urlopen".equals(calledFunction)) {

                library = "urllib";
                method = "urlopen";

            } else {

                return;
            }

            String urlExpression = null;

            if ("requests".equals(library) && "request".equals(method)) {

                if (!arguments.isEmpty()) {

                    if (arguments.get(0).startsWith("method=")) {

                        urlExpression = findKeywordArgument(
                                arguments,
                                "url"
                        );

                    } else if (arguments.size() > 1) {

                        urlExpression = extractArgumentValue(
                                arguments.get(1)
                        );
                    }
                }

            } else {

                urlExpression = findKeywordArgument(arguments, "url");

                if (urlExpression == null && !arguments.isEmpty()) {

                    urlExpression = extractArgumentValue(arguments.get(0));
                }
            }

            if (urlExpression == null) {
                urlExpression = "<dynamic-or-unresolved-url>";
            }

            outboundHttpCalls.add(
                    new PythonOutboundHttpCallInfo(
                            library,
                            method,
                            urlExpression,
                            arguments,
                            sourceLocation
                    )
            );
        }

        private String findKeywordArgument(
                List<String> arguments,
                String keyword
        ) {

            String prefix = keyword + "=";

            for (String argument : arguments) {

                String trimmed = argument.trim();

                if (trimmed.startsWith(prefix)) {

                    return trimmed.substring(prefix.length()).trim();
                }
            }

            return null;
        }

        private String extractArgumentValue(String argument) {

            if (argument == null) {
                return null;
            }

            String trimmed = argument.trim();

            int equalsIndex = trimmed.indexOf('=');

            if (equalsIndex > 0) {

                String possibleKeyword =
                        trimmed.substring(0, equalsIndex).trim();

                if (possibleKeyword.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {

                    return trimmed.substring(equalsIndex + 1).trim();
                }
            }

            return trimmed;
        }

        private String extractStringValue(String expression) {

            if (expression == null) {
                return null;
            }

            String value = expression.trim();

            Matcher matcher = Pattern.compile(
                    "^(?:[rRuU]{0,2})?(\"\"\"|'''|\"|')([\\s\\S]*)\\1$"
            ).matcher(value);

            if (matcher.matches()) {
                return matcher.group(2);
            }

            return null;
        }

        // ---------------------------------------------------------
        // RAISED EXCEPTION ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitRaise_stmt(
                Python3Parser.Raise_stmtContext ctx
        ) {

            List<Python3Parser.TestContext> tests = ctx.test();

            if (tests == null || tests.isEmpty()) {

                raisedExceptions.add(
                        new PythonRaisedExceptionInfo(
                                null,
                                null,
                                null,
                                true,
                                location(ctx)
                        )
                );

                return visitChildren(ctx);
            }

            String exceptionExpression =
                    PythonAstTextExtractor.getText(tests.get(0));

            String causeExpression = tests.size() > 1
                    ? PythonAstTextExtractor.getText(tests.get(1))
                    : null;

            String exceptionType =
                    extractExceptionType(exceptionExpression);

            raisedExceptions.add(
                    new PythonRaisedExceptionInfo(
                            exceptionType,
                            exceptionExpression,
                            causeExpression,
                            false,
                            location(ctx)
                    )
            );

            return visitChildren(ctx);
        }

        private String extractExceptionType(String expression) {

            if (expression == null || expression.isBlank()) {
                return null;
            }

            String value = expression.trim();

            int openingParenthesis = value.indexOf('(');

            if (openingParenthesis >= 0) {

                return value.substring(0, openingParenthesis).trim();
            }

            return value;
        }

        // ---------------------------------------------------------
        // CONTROL FLOW ANALYSIS
        // ---------------------------------------------------------

        @Override
        public Void visitIf_stmt(
                Python3Parser.If_stmtContext ctx
        ) {

            controlFlow.add(new PythonControlFlowInfo(
                    "IF",
                    PythonAstTextExtractor.getText(ctx.test(0)),
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        @Override
        public Void visitWhile_stmt(
                Python3Parser.While_stmtContext ctx
        ) {

            controlFlow.add(new PythonControlFlowInfo(
                    "WHILE",
                    PythonAstTextExtractor.getText(ctx.test()),
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        @Override
        public Void visitFor_stmt(
                Python3Parser.For_stmtContext ctx
        ) {

            controlFlow.add(new PythonControlFlowInfo(
                    "FOR",
                    PythonAstTextExtractor.getText(ctx.exprlist())
                            + " in "
                            + PythonAstTextExtractor.getText(ctx.testlist()),
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        @Override
        public Void visitTry_stmt(
                Python3Parser.Try_stmtContext ctx
        ) {

            controlFlow.add(new PythonControlFlowInfo(
                    "TRY",
                    null,
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        @Override
        public Void visitWith_stmt(
                Python3Parser.With_stmtContext ctx
        ) {

            String condition = ctx.with_item()
                    .stream()
                    .map(PythonAstTextExtractor::getText)
                    .reduce((first, second) ->
                            first + ", " + second
                    )
                    .orElse(null);

            controlFlow.add(new PythonControlFlowInfo(
                    "WITH",
                    condition,
                    location(ctx)
            ));

            return visitChildren(ctx);
        }

        // ---------------------------------------------------------
        // SOURCE LOCATION
        // ---------------------------------------------------------

        private static SourceLocation location(
                ParserRuleContext ctx
        ) {

            return new SourceLocation(
                    PythonAstTextExtractor.getStartLine(ctx),
                    PythonAstTextExtractor.getEndLine(ctx)
            );
        }
    }

    // -------------------------------------------------------------
    // FUNCTION BUILDER
    // -------------------------------------------------------------

    private static class FunctionBuilder {

        private final String name;
        private final List<String> decorators;
        private final List<PythonParameterInfo> parameters;
        private final String returnAnnotation;
        private final boolean asynchronous;
        private final boolean method;
        private final SourceLocation location;
        private final int classDepth;

        private String qualifiedName;

        private final List<FunctionBuilder> nestedFunctions =
                new ArrayList<>();

        private FunctionBuilder(
                String name,
                List<String> decorators,
                List<PythonParameterInfo> parameters,
                String returnAnnotation,
                boolean asynchronous,
                boolean method,
                SourceLocation location,
                int classDepth
        ) {

            this.name = name;
            this.decorators = decorators;
            this.parameters = parameters;
            this.returnAnnotation = returnAnnotation;
            this.asynchronous = asynchronous;
            this.method = method;
            this.location = location;
            this.classDepth = classDepth;
        }

        private PythonFunctionInfo build() {

            List<PythonFunctionInfo> nested =
                    nestedFunctions.stream()
                            .map(FunctionBuilder::build)
                            .toList();

            return new PythonFunctionInfo(
                    name,
                    decorators,
                    parameters,
                    returnAnnotation,
                    asynchronous,
                    method,
                    location,
                    nested
            );
        }
    }

    // -------------------------------------------------------------
    // CLASS BUILDER
    // -------------------------------------------------------------

    private static class ClassBuilder {

        private final String name;
        private final List<String> decorators;
        private final List<String> baseClasses;
        private final SourceLocation location;

        private final List<PythonFunctionInfo> methods =
                new ArrayList<>();

        private final List<PythonVariableInfo> attributes =
                new ArrayList<>();

        private ClassBuilder(
                String name,
                List<String> decorators,
                List<String> baseClasses,
                SourceLocation location
        ) {

            this.name = name;
            this.decorators = decorators;
            this.baseClasses = baseClasses;
            this.location = location;
        }

        private PythonClassInfo build() {

            return new PythonClassInfo(
                    name,
                    decorators,
                    baseClasses,
                    methods,
                    attributes,
                    location
            );
        }
    }
}
