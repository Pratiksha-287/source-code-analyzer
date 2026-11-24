
package org.example.sourcecodeanalyser.analyzer.frontend;

import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstStr;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleExportName;
import com.caoccao.javet.swc4j.ast.module.*;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitor;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitorResponse;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;

import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstClassDecl;

import org.example.sourcecodeanalyser.model.frontend.ImportExportAnalysis;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImportExportAnalyzer {

    public ImportExportAnalysis analyze(Swc4jParseOutput parseOutput) {

        List<ImportExportAnalysis.ImportInfo> imports =
                new ArrayList<>();

        List<ImportExportAnalysis.ExportInfo> exports =
                new ArrayList<>();

        Swc4jAstVisitor visitor = new Swc4jAstVisitor() {

            // ---------------------------------
            // IMPORT DECLARATIONS
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitImportDecl(
                    Swc4jAstImportDecl node) {

                List<String> specifiers = new ArrayList<>();

                List<ImportExportAnalysis.ImportSpecifierInfo>
                        structuredSpecifiers = new ArrayList<>();

                for (var specifier : node.getSpecifiers()) {

                    if (specifier instanceof Swc4jAstImportDefaultSpecifier defaultSpecifier) {

                        String localName = defaultSpecifier.getLocal().getSym();

                        specifiers.add("default: " + localName);

                        structuredSpecifiers.add(
                                new ImportExportAnalysis.ImportSpecifierInfo(
                                        "default",
                                        "default",
                                        localName
                                )
                        );

                    } else if (specifier instanceof Swc4jAstImportNamedSpecifier namedSpecifier) {

                        String importedName = namedSpecifier
                                .getImported()
                                .map(ImportExportAnalyzer::getExportName)
                                .orElse(namedSpecifier.getLocal().getSym());

                        String localName = namedSpecifier.getLocal().getSym();

                        specifiers.add(
                                "named: " + importedName +
                                        " as " + localName
                        );

                        structuredSpecifiers.add(
                                new ImportExportAnalysis.ImportSpecifierInfo(
                                        "named",
                                        importedName,
                                        localName
                                )
                        );

                    } else if (specifier instanceof Swc4jAstImportStarAsSpecifier namespaceSpecifier) {

                        String localName = namespaceSpecifier.getLocal().getSym();

                        specifiers.add("namespace: " + localName);

                        structuredSpecifiers.add(
                                new ImportExportAnalysis.ImportSpecifierInfo(
                                        "namespace",
                                        "*",
                                        localName
                                )
                        );
                    }
                }

                imports.add(
                        new ImportExportAnalysis.ImportInfo(
                                node.getSrc().getValue(),
                                List.copyOf(specifiers),
                                node.isTypeOnly(),
                                List.copyOf(structuredSpecifiers)
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            // ---------------------------------
            // EXPORT DECLARATIONS
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitExportDecl(
                    Swc4jAstExportDecl node) {

                List<String> names = new ArrayList<>();

                var declaration = node.getDecl();

                if (declaration instanceof Swc4jAstVarDecl varDecl) {

                    for (var declarator : varDecl.getDecls()) {

                        var name = declarator.getName();

                        if (name instanceof Swc4jAstBindingIdent bindingIdent) {
                            names.add(bindingIdent.getId().getSym());
                        } else {
                            names.add(name.toString());
                        }
                    }

                } else if (declaration instanceof Swc4jAstFnDecl fnDecl) {

                    names.add(fnDecl.getIdent().getSym());

                } else if (declaration instanceof Swc4jAstClassDecl classDecl) {

                    names.add(classDecl.getIdent().getSym());

                } else {

                    names.add(declaration.getType().name());
                }

                exports.add(
                        new ImportExportAnalysis.ExportInfo(
                                "DECLARATION",
                                null,
                                List.copyOf(names),
                                false
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            // ---------------------------------
            // DEFAULT EXPORT DECLARATION
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitExportDefaultDecl(
                    Swc4jAstExportDefaultDecl node) {

                exports.add(
                        new ImportExportAnalysis.ExportInfo(
                                "DEFAULT_DECLARATION",
                                null,
                                List.of(),
                                false
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            // ---------------------------------
            // DEFAULT EXPORT EXPRESSION
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitExportDefaultExpr(
                    Swc4jAstExportDefaultExpr node) {

                exports.add(
                        new ImportExportAnalysis.ExportInfo(
                                "DEFAULT_EXPRESSION",
                                null,
                                List.of(),
                                false
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            // ---------------------------------
            // NAMED EXPORTS AND RE-EXPORTS
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitNamedExport(
                    Swc4jAstNamedExport node) {

                List<String> names = new ArrayList<>();

                for (var specifier : node.getSpecifiers()) {

                    if (specifier instanceof Swc4jAstExportNamedSpecifier namedSpecifier) {

                        String originalName =
                                getExportName(namedSpecifier.getOrig());

                        String exportedName = namedSpecifier
                                .getExported()
                                .map(ImportExportAnalyzer::getExportName)
                                .orElse(originalName);

                        names.add(
                                originalName.equals(exportedName)
                                        ? originalName
                                        : originalName + " as " + exportedName
                        );

                    } else if (specifier instanceof Swc4jAstExportDefaultSpecifier defaultSpecifier) {

                        names.add(
                                "default as " +
                                        defaultSpecifier.getExported().getSym()
                        );

                    } else if (specifier instanceof Swc4jAstExportNamespaceSpecifier namespaceSpecifier) {

                        names.add(
                                "* as " +
                                        getExportName(namespaceSpecifier.getName())
                        );
                    }
                }

                exports.add(
                        new ImportExportAnalysis.ExportInfo(
                                "NAMED",
                                node.getSrc()
                                        .map(Swc4jAstStr::getValue)
                                        .orElse(null),
                                List.copyOf(names),
                                node.isTypeOnly()
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }

            // ---------------------------------
            // EXPORT ALL
            // ---------------------------------

            @Override
            public Swc4jAstVisitorResponse visitExportAll(
                    Swc4jAstExportAll node) {

                exports.add(
                        new ImportExportAnalysis.ExportInfo(
                                "ALL",
                                node.getSrc().getValue(),
                                List.of("*"),
                                node.isTypeOnly()
                        )
                );

                return Swc4jAstVisitorResponse.OkAndContinue;
            }
        };

        parseOutput.getProgram().visit(visitor);

        return new ImportExportAnalysis(
                List.copyOf(imports),
                List.copyOf(exports)
        );
    }

    private static String getExportName(
            ISwc4jAstModuleExportName exportName) {

        if (exportName instanceof Swc4jAstIdent ident) {
            return ident.getSym();
        }

        if (exportName instanceof Swc4jAstStr string) {
            return string.getValue();
        }

        return exportName.toString();
    }
}
