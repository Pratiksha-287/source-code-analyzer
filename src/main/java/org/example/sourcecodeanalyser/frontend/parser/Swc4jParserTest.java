
package org.example.sourcecodeanalyser.frontend.parser;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.enums.Swc4jMediaType;
import com.caoccao.javet.swc4j.options.Swc4jParseOptions;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import com.caoccao.javet.swc4j.enums.Swc4jParseMode;

public class Swc4jParserTest {

    public static void main(String[] args) throws Exception {

        String sourceCode = """
                import React from 'react';

                interface Props {
                    name: string;
                }

                const App = ({ name }: Props) => {
                    return <h1>Hello {name}</h1>;
                };

                export default App;
                """;

        Swc4j swc4j = new Swc4j();

        Swc4jParseOptions options = new Swc4jParseOptions()
                .setMediaType(Swc4jMediaType.Tsx)
                .setParseMode(Swc4jParseMode.Module)
                .setCaptureAst(true);

        Swc4jParseOutput output = swc4j.parse(sourceCode, options);

        System.out.println("Parsing successful!");
        System.out.println(output.getProgram().toDebugString());
    }
}
