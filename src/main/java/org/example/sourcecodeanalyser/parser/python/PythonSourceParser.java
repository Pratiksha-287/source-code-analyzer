
package org.example.sourcecodeanalyser.parser.python;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PythonSourceParser {

    public ParseResult parse(Path filePath) throws IOException {

        Python3Lexer lexer = new Python3Lexer(
                CharStreams.fromPath(filePath)
        );

        List<SyntaxErrorInfo> errors = new ArrayList<>();

        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(errors));

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        Python3Parser parser = new Python3Parser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(errors));

        ParseTree parseTree = parser.file_input();

        return new ParseResult(parseTree, errors);
    }

    public record ParseResult(
            ParseTree parseTree,
            List<SyntaxErrorInfo> errors
    ) {
        public ParseResult {
            errors = List.copyOf(errors);
        }
    }

    public record SyntaxErrorInfo(
            String message,
            int line,
            int column
    ) {}

    private static class CollectingErrorListener extends BaseErrorListener {

        private final List<SyntaxErrorInfo> errors;

        private CollectingErrorListener(List<SyntaxErrorInfo> errors) {
            this.errors = errors;
        }

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException exception
        ) {
            errors.add(
                    new SyntaxErrorInfo(msg, line, charPositionInLine)
            );
        }
    }
}
/* What this class does
1. Reads the Python file.
2. Tokenizes it using Python3Lexer.
3. Parses the tokens using Python3Parser.
4. Collects lexer and parser syntax errors.
5. Returns the parse tree and errors together. */
