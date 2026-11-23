
package org.example.sourcecodeanalyser.analyzer.frontend;

import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstFunction;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstParam;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstArrowExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstFnExpr;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDecl;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitor;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitorResponse;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;

import org.example.sourcecodeanalyser.model.frontend.FunctionAnalysis;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Component
public class FunctionAnalyzer {

    public FunctionAnalysis analyze(Swc4jParseOutput parseOutput) {

        List<FunctionAnalysis.FunctionInfo> functions = new ArrayList<>();

        Set<Object> handledFunctions =
                Collections.newSetFromMap(new IdentityHashMap<>());

        Swc4jAstVisitor visitor = new Swc4jAstVisitor() {

            @Override
            public Swc4jAstVisitorResponse visitFnDecl(Swc4jAstFnDecl node) {

                Swc4jAstFunction function = node.getFunction();

                functions.add(new FunctionAnalysis.FunctionInfo(
                        node.getIdent().getSym(),
                        "DECLARATION",
                        extractParameters(function.getParams()),
                        function.isAsync()
                ));

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            @Override
            public Swc4jAstVisitorResponse visitVarDecl(Swc4jAstVarDecl node) {

                for (var declarator : node.getDecls()) {

                    if (!(declarator.getName()
                            instanceof Swc4jAstBindingIdent bindingIdent)) {
                        continue;
                    }

                    String variableName = bindingIdent.getId().getSym();

                    declarator.getInit().ifPresent(initializer -> {

                        if (initializer instanceof Swc4jAstArrowExpr arrow) {

                            handledFunctions.add(arrow);

                            functions.add(new FunctionAnalysis.FunctionInfo(
                                    variableName,
                                    "ARROW",
                                    extractArrowParameters(arrow),
                                    arrow.isAsync()
                            ));

                        } else if (initializer instanceof Swc4jAstFnExpr fnExpr) {

                            handledFunctions.add(fnExpr);

                            functions.add(new FunctionAnalysis.FunctionInfo(
                                    fnExpr.getIdent()
                                            .map(ident -> ident.getSym())
                                            .orElse(variableName),
                                    "EXPRESSION",
                                    extractParameters(
                                            fnExpr.getFunction().getParams()
                                    ),
                                    fnExpr.getFunction().isAsync()
                            ));
                        }
                    });
                }

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            @Override
            public Swc4jAstVisitorResponse visitArrowExpr(Swc4jAstArrowExpr node) {

                if (!handledFunctions.contains(node)) {

                    functions.add(new FunctionAnalysis.FunctionInfo(
                            "<anonymous>",
                            "ARROW",
                            extractArrowParameters(node),
                            node.isAsync()
                    ));
                }

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            @Override
            public Swc4jAstVisitorResponse visitFnExpr(Swc4jAstFnExpr node) {

                if (!handledFunctions.contains(node)) {

                    functions.add(new FunctionAnalysis.FunctionInfo(
                            node.getIdent()
                                    .map(ident -> ident.getSym())
                                    .orElse("<anonymous>"),
                            "EXPRESSION",
                            extractParameters(
                                    node.getFunction().getParams()
                            ),
                            node.getFunction().isAsync()
                    ));
                }

                return Swc4jAstVisitorResponse.OkAndContinue;
            }
        };

        parseOutput.getProgram().visit(visitor);

        return new FunctionAnalysis(List.copyOf(functions));
    }

    private List<String> extractParameters(List<Swc4jAstParam> params) {

        List<String> parameters = new ArrayList<>();

        for (Swc4jAstParam param : params) {

            var pattern = param.getPat();

            if (pattern instanceof Swc4jAstBindingIdent bindingIdent) {
                parameters.add(bindingIdent.getId().getSym());
            } else {
                parameters.add(pattern.getType().name());
            }
        }

        return List.copyOf(parameters);
    }

    private List<String> extractArrowParameters(Swc4jAstArrowExpr arrow) {

        List<String> parameters = new ArrayList<>();

        for (var pattern : arrow.getParams()) {

            if (pattern instanceof Swc4jAstBindingIdent bindingIdent) {
                parameters.add(bindingIdent.getId().getSym());
            } else {
                parameters.add(pattern.getType().name());
            }
        }

        return List.copyOf(parameters);
    }
}
