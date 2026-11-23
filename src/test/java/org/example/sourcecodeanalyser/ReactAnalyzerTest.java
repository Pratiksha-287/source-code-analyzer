
package org.example.sourcecodeanalyser;

import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.example.sourcecodeanalyser.analyzer.frontend.ImportExportAnalyzer;
import org.example.sourcecodeanalyser.analyzer.frontend.ReactAnalyzer;
import org.example.sourcecodeanalyser.frontend.parser.Swc4jParser;
import org.example.sourcecodeanalyser.model.frontend.ReactAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReactAnalyzerTest {

    @TempDir
    Path tempDir;

    private ReactAnalyzer reactAnalyzer;
    private Swc4jParser swc4jParser;

    @BeforeEach
    void setUp() {
        reactAnalyzer = new ReactAnalyzer(new ImportExportAnalyzer());
        swc4jParser = new Swc4jParser();
    }

    private Swc4jParseOutput parse(String source) throws Exception {
        Path file = tempDir.resolve("TestComponent.jsx");

        Files.writeString(
                file,
                source,
                StandardCharsets.UTF_8
        );

        return swc4jParser.parse(file);
    }

    // ---------------------------------------------------------
    // Component and hook detection tests
    // ---------------------------------------------------------

    @Test
    void shouldDetectFunctionComponentsAndHooks() throws Exception {

        String source = """
                export function Dashboard() {

                    const [count, setCount] = useState(0);

                    useEffect(() => {
                        console.log(count);
                    }, [count]);

                    return <div>{count}</div>;
                }

                export const UserProfile = () => {

                    const auth = useAuth();

                    return <div>{auth.user}</div>;
                };
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(2, analysis.components().size());

        ReactAnalysis.ComponentInfo dashboard =
                findComponent(analysis, "Dashboard");

        ReactAnalysis.ComponentInfo userProfile =
                findComponent(analysis, "UserProfile");

        assertEquals("FUNCTION_DECLARATION", dashboard.kind());
        assertEquals("ARROW", userProfile.kind());

        assertTrue(hasHook(analysis, "useState", "Dashboard"));
        assertTrue(hasHook(analysis, "useEffect", "Dashboard"));
        assertTrue(hasHook(analysis, "useAuth", "UserProfile"));

        ReactAnalysis.HookInfo customHook =
                findHook(analysis, "useAuth", "UserProfile");

        assertTrue(customHook.custom());
    }

    @Test
    void shouldFormatHookArgumentsAsReadableStrings() throws Exception {

        String source = """
                function Dashboard() {

                    const [count, setCount] = useState(0);

                    useEffect(() => {
                        console.log(count);
                    }, [count]);

                    const doubled = useMemo(
                        () => count * 2,
                        [count]
                    );

                    return <div>{doubled}</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        ReactAnalysis.HookInfo stateHook =
                findHook(analysis, "useState", "Dashboard");

        ReactAnalysis.HookInfo effectHook =
                findHook(analysis, "useEffect", "Dashboard");

        ReactAnalysis.HookInfo memoHook =
                findHook(analysis, "useMemo", "Dashboard");

        assertEquals(List.of("0"), stateHook.arguments());

        assertEquals(
                List.of("<callback>", "[count]"),
                effectHook.arguments()
        );

        assertEquals(
                List.of("<callback>", "[count]"),
                memoHook.arguments()
        );
    }

    @Test
    void shouldDetectFunctionExpressionComponent() throws Exception {

        String source = """
                const UserCard = function() {
                    return <div>User card</div>;
                };
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        ReactAnalysis.ComponentInfo component =
                findComponent(analysis, "UserCard");

        assertEquals("FUNCTION_EXPRESSION", component.kind());
    }

    @Test
    void shouldDetectReactClassComponent() throws Exception {

        String source = """
                class Welcome extends Component {
                    render() {
                        return <h1>Welcome</h1>;
                    }
                }

                class UserDashboard extends React.PureComponent {
                    render() {
                        return <div>Dashboard</div>;
                    }
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(2, analysis.components().size());

        assertEquals(
                "CLASS",
                findComponent(analysis, "Welcome").kind()
        );

        assertEquals(
                "CLASS",
                findComponent(analysis, "UserDashboard").kind()
        );
    }

    @Test
    void shouldNotDetectOrdinaryClassAsReactComponent() throws Exception {

        String source = """
                class UserService {
                    getUser() {
                        return "User";
                    }
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertTrue(analysis.components().isEmpty());
    }

    @Test
    void shouldNotDetectLowercaseFunctionAsComponent() throws Exception {

        String source = """
                function userProfile() {
                    return <div>User profile</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertTrue(analysis.components().isEmpty());
    }

    // ---------------------------------------------------------
    // Props extraction tests
    // ---------------------------------------------------------

    @Test
    void shouldExtractDestructuredPropsFromFunctionComponent()
            throws Exception {

        String source = """
                function UserProfile({
                    userId,
                    name: displayName,
                    role = "USER",
                    ...rest
                }) {
                    return <div>{displayName}</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        ReactAnalysis.ComponentInfo component =
                findComponent(analysis, "UserProfile");

        assertEquals(
                List.of("userId", "name", "role", "...rest"),
                component.props()
        );
    }

    @Test
    void shouldExtractPropsFromArrowComponent() throws Exception {

        String source = """
                const UserCard = (props) => {
                    return <div>{props.name}</div>;
                };
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        ReactAnalysis.ComponentInfo component =
                findComponent(analysis, "UserCard");

        assertEquals(List.of("props"), component.props());
    }

    // ---------------------------------------------------------
    // State extraction tests
    // ---------------------------------------------------------

    @Test
    void shouldExtractMultipleUseStateDeclarations()
            throws Exception {

        String source = """
                function Dashboard() {

                    const [count, setCount] = useState(0);

                    const [name, setName] = useState("Guest");

                    const [active, setActive] = useState(true);

                    return <div>
                        {count}
                        {name}
                        {active}
                    </div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        ReactAnalysis.ComponentInfo component =
                findComponent(analysis, "Dashboard");

        assertEquals(3, component.state().size());

        assertEquals(
                new ReactAnalysis.StateInfo(
                        "count",
                        "setCount",
                        "useState",
                        "0"
                ),
                component.state().get(0)
        );

        assertEquals(
                new ReactAnalysis.StateInfo(
                        "name",
                        "setName",
                        "useState",
                        "\"Guest\""
                ),
                component.state().get(1)
        );

        assertEquals(
                new ReactAnalysis.StateInfo(
                        "active",
                        "setActive",
                        "useState",
                        "true"
                ),
                component.state().get(2)
        );
    }

    // ---------------------------------------------------------
    // API interaction extraction tests
    // ---------------------------------------------------------

    @Test
    void shouldDetectFetchGetRequest() throws Exception {

        String source = """
                function UserList() {

                    const loadUsers = async () => {
                        const response = await fetch("/api/users");
                        return response.json();
                    };

                    return <div>Users</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("UserList", interaction.componentName());
        assertEquals("fetch", interaction.client());
        assertEquals("GET", interaction.method());
        assertEquals("/api/users", interaction.endpoint());
    }

    @Test
    void shouldDetectFetchPostRequest() throws Exception {

        String source = """
                function CreateUser() {

                    const createUser = async (user) => {
                        const response = await fetch("/api/users", {
                            method: "POST",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify(user)
                        });

                        return response.json();
                    };

                    return <div>Create user</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("CreateUser", interaction.componentName());
        assertEquals("fetch", interaction.client());
        assertEquals("POST", interaction.method());
        assertEquals("/api/users", interaction.endpoint());
    }

    @Test
    void shouldDetectAxiosGetRequest() throws Exception {

        String source = """
                function UserList() {

                    const loadUsers = async () => {
                        const response = await axios.get("/api/users");
                        return response.data;
                    };

                    return <div>Users</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("UserList", interaction.componentName());
        assertEquals("axios", interaction.client());
        assertEquals("GET", interaction.method());
        assertEquals("/api/users", interaction.endpoint());
    }


    @Test
    void shouldDetectDynamicApiEndpoints() throws Exception {

        String source = """
            function UserDetails({ userId }) {

                const loadUser = async () => {
                    await fetch(`/api/users/${userId}`);
                };

                return <div>User details</div>;
            }
            """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("UserDetails", interaction.componentName());
        assertEquals("fetch", interaction.client());
        assertEquals("GET", interaction.method());

        assertTrue(interaction.endpoint().contains("/api/users/"));

        System.out.println("Extracted endpoint: " + interaction.endpoint());
        assertTrue(interaction.endpoint().contains("userId"));
    }

    @Test
    void shouldDetectAxiosPostRequest() throws Exception {

        String source = """
                function CreateUser() {

                    const createUser = async (user) => {
                        const response = await axios.post(
                            "/api/users",
                            user
                        );

                        return response.data;
                    };

                    return <div>Create user</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("CreateUser", interaction.componentName());
        assertEquals("axios", interaction.client());
        assertEquals("POST", interaction.method());
        assertEquals("/api/users", interaction.endpoint());

    }

    @Test
    void shouldDetectAxiosConfigurationObjectRequest() throws Exception {

        String source = """
        function UserDetails({ userId }) {

            const loadUser = async () => {
                await axios({
                    url: `/api/users/${userId}`,
                    method: "POST"
                });
            };

            return <div>User details</div>;
        }
        """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(1, analysis.apiInteractions().size());

        ReactAnalysis.ApiInteractionInfo interaction =
                analysis.apiInteractions().get(0);

        assertEquals("UserDetails", interaction.componentName());
        assertEquals("axios", interaction.client());
        assertEquals("POST", interaction.method());
        assertEquals("/api/users/${userId}", interaction.endpoint());

        System.out.println(
                "Axios configuration request: " + interaction
        );
    }

    @Test
    void shouldDetectMultipleApiCallsInOneComponent()
            throws Exception {

        String source = """
                function UserDashboard() {

                    const loadUsers = async () => {
                        await fetch("/api/users");
                    };

                    const loadRoles = async () => {
                        await axios.get("/api/roles");
                    };

                    const createUser = async (user) => {
                        await axios.post("/api/users", user);
                    };

                    return <div>User dashboard</div>;
                }
                """;

        ReactAnalysis analysis =
                reactAnalyzer.analyze(parse(source));

        assertEquals(3, analysis.apiInteractions().size());

        assertTrue(
                analysis.apiInteractions().stream()
                        .allMatch(interaction ->
                                interaction.componentName()
                                        .equals("UserDashboard")
                        )
        );

        assertTrue(
                analysis.apiInteractions().stream()
                        .anyMatch(interaction ->
                                interaction.client().equals("fetch")
                                        && interaction.method().equals("GET")
                                        && interaction.endpoint()
                                        .equals("/api/users")
                        )
        );

        assertTrue(
                analysis.apiInteractions().stream()
                        .anyMatch(interaction ->
                                interaction.client().equals("axios")
                                        && interaction.method().equals("GET")
                                        && interaction.endpoint()
                                        .equals("/api/roles")
                        )
        );

        assertTrue(
                analysis.apiInteractions().stream()
                        .anyMatch(interaction ->
                                interaction.client().equals("axios")
                                        && interaction.method().equals("POST")
                                        && interaction.endpoint()
                                        .equals("/api/users")
                        )
        );
    }

    // ---------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------

    private ReactAnalysis.ComponentInfo findComponent(
            ReactAnalysis analysis,
            String componentName) {

        return analysis.components()
                .stream()
                .filter(component ->
                        component.name().equals(componentName))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Component not found: " + componentName
                        )
                );
    }

    private boolean hasHook(
            ReactAnalysis analysis,
            String hookName,
            String componentName) {

        return analysis.hooks()
                .stream()
                .anyMatch(hook ->
                        hook.name().equals(hookName)
                                && hook.componentName().equals(componentName)
                );
    }

    private ReactAnalysis.HookInfo findHook(
            ReactAnalysis analysis,
            String hookName,
            String componentName) {

        return analysis.hooks()
                .stream()
                .filter(hook ->
                        hook.name().equals(hookName)
                                && hook.componentName().equals(componentName)
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Hook not found: "
                                        + hookName
                                        + " in "
                                        + componentName
                        )
                );
    }
}
