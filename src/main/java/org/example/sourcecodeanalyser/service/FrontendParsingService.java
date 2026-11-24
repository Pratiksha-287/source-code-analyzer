
package org.example.sourcecodeanalyser.service;

import com.caoccao.javet.swc4j.exceptions.Swc4jCoreException;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.example.sourcecodeanalyser.model.FrontendFileParseResult;
import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class FrontendParsingService {

    private final Swc4jParser swc4jParser;

    public FrontendParsingService(Swc4jParser swc4jParser) {
        this.swc4jParser = swc4jParser;
    }

    public List<FrontendFileParseResult> parseFiles(List<Path> files) {

        List<FrontendFileParseResult> results = new ArrayList<>();

        for (Path file : files) {

            try {
                Swc4jParseOutput output = swc4jParser.parse(file);

                results.add(
                        new FrontendFileParseResult(
                                file,
                                output,
                                null
                        )
                );

                System.out.println("Parsed successfully: " + file);

            } catch (IOException | Swc4jCoreException exception) {

                results.add(
                        new FrontendFileParseResult(
                                file,
                                null,
                                exception.getMessage()
                        )
                );

                System.err.println(
                        "Failed to parse: " + file
                                + " - " + exception.getMessage()
                );
            }
        }

        return results;
    }
}
