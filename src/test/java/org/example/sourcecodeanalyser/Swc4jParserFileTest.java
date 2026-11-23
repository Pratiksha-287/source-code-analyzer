
package org.example.sourcecodeanalyser;

import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class Swc4jParserFileTest {

    @TempDir
    Path tempDir;

    private final Swc4jParser parser = new Swc4jParser();

    @Test
    void parsesJavaScriptFile() throws Exception {

        String sourceCode = """
                const message = "Hello";

                function greet(name) {
                    return message + " " + name;
                }
                """;

        assertFileParses("sample.js", sourceCode);
    }
    @Test
    void parsesJsxInsideJavaScriptFile() throws Exception {

        String sourceCode = """
            import React from "react";

            function App() {
                return <div>Hello GovCon</div>;
            }

            export default App;
            """;

        assertFileParses("App.js", sourceCode);
    }
    @Test
    void parsesJsxFile() throws Exception {

        String sourceCode = """
                const App = () => {
                    return <h1>Hello React</h1>;
                };

                export default App;
                """;

        assertFileParses("App.jsx", sourceCode);
    }

    @Test
    void parsesTypeScriptFile() throws Exception {

        String sourceCode = """
                interface User {
                    id: number;
                    name: string;
                }

                const user: User = {
                    id: 1,
                    name: "Deepanshi"
                };
                """;

        assertFileParses("User.ts", sourceCode);
    }

    @Test
    void parsesTsxFile() throws Exception {

        String sourceCode = """
                import React from "react";

                interface Props {
                    name: string;
                }

                const App = ({ name }: Props) => {
                    return <h1>Hello {name}</h1>;
                };

                export default App;
                """;

        assertFileParses("App.tsx", sourceCode);
    }

    private void assertFileParses(
            String fileName,
            String sourceCode
    ) throws Exception {

        Path filePath = tempDir.resolve(fileName);

        Files.writeString(filePath, sourceCode);

        Swc4jParseOutput output = parser.parse(filePath);

        assertNotNull(output, fileName + " should produce a parse output");
        assertNotNull(output.getProgram(), fileName + " should produce an AST");

        System.out.println(
                "\nSuccessfully parsed: " + fileName
        );

        System.out.println(output.getProgram());
    }
}
