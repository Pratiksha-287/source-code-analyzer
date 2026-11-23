
package org.example.sourcecodeanalyser.parser.python;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;

public final class PythonAstTextExtractor {

    private PythonAstTextExtractor() {
    }

    public static String getText(ParseTree tree) {

        if (tree == null) {
            return null;
        }

        if (tree instanceof ParserRuleContext context) {

            Token start = context.getStart();
            Token stop = context.getStop();

            if (start != null && stop != null) {

                int startIndex = start.getStartIndex();
                int stopIndex = stop.getStopIndex();

                if (startIndex >= 0 && stopIndex >= startIndex) {

                    CharStream input = start.getTokenSource() == null
                            ? null
                            : start.getTokenSource().getInputStream();

                    if (input != null) {
                        String sourceText = input.getText(
                                Interval.of(startIndex, stopIndex)
                        );

                        if (sourceText != null && !sourceText.isBlank()) {
                            return sourceText;
                        }
                    }
                }
            }
        }

        // Fallback for parse-tree nodes without usable source positions.
        String text = tree.getText();

        return text == null || text.isBlank()
                ? null
                : text;
    }

    public static int getStartLine(ParserRuleContext context) {

        Token token = context.getStart();

        return token == null ? 0 : token.getLine();
    }

    public static int getStartColumn(ParserRuleContext context) {

        Token token = context.getStart();

        return token == null ? 0 : token.getCharPositionInLine();
    }

    public static int getEndLine(ParserRuleContext context) {

        Token token = context.getStop();

        return token == null
                ? getStartLine(context)
                : token.getLine();
    }

    public static int getEndColumn(ParserRuleContext context) {

        Token token = context.getStop();

        if (token == null) {
            return getStartColumn(context);
        }

        return token.getCharPositionInLine()
                + token.getText().length();
    }
}
