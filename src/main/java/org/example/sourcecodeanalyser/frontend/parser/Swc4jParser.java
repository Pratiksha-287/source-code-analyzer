
package org.example.sourcecodeanalyser.frontend.parser;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.enums.Swc4jMediaType;
import com.caoccao.javet.swc4j.enums.Swc4jParseMode;
import com.caoccao.javet.swc4j.exceptions.Swc4jCoreException;
import com.caoccao.javet.swc4j.options.Swc4jParseOptions;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class Swc4jParser {

    public Swc4jParseOutput parse(Path filePath)
            throws IOException, Swc4jCoreException {

        String sourceCode = Files.readString(
                filePath,
                StandardCharsets.UTF_8
        );

        Swc4jMediaType mediaType = resolveMediaType(filePath);

        Swc4jParseOptions options = new Swc4jParseOptions()
                .setMediaType(mediaType)
                .setParseMode(Swc4jParseMode.Module)
                .setCaptureAst(true);

        Swc4j swc4j = new Swc4j();

        return swc4j.parse(sourceCode, options);
    } // parse() ends here

    private Swc4jMediaType resolveMediaType(Path filePath) {

        String fileName = filePath.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".d.mts")) {
            return Swc4jMediaType.Dmts;
        }

        if (fileName.endsWith(".d.cts")) {
            return Swc4jMediaType.Dcts;
        }

        if (fileName.endsWith(".d.ts")) {
            return Swc4jMediaType.Dts;
        }

        if (fileName.endsWith(".mjs")) {
            return Swc4jMediaType.Mjs;
        }

        if (fileName.endsWith(".cjs")) {
            return Swc4jMediaType.Cjs;
        }

        if (fileName.endsWith(".jsx")) {
            return Swc4jMediaType.Jsx;
        }

        if (fileName.endsWith(".js")) {
            return Swc4jMediaType.Jsx;
        }

        if (fileName.endsWith(".mts")) {
            return Swc4jMediaType.Mts;
        }

        if (fileName.endsWith(".cts")) {
            return Swc4jMediaType.Cts;
        }

        if (fileName.endsWith(".tsx")) {
            return Swc4jMediaType.Tsx;
        }

        if (fileName.endsWith(".ts")) {
            return Swc4jMediaType.TypeScript;
        }

        throw new IllegalArgumentException(
                "Unsupported frontend file: " + filePath
        );
    }
}
