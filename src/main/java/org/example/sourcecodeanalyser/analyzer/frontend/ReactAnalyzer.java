
package org.example.sourcecodeanalyser.analyzer.frontend;

import com.caoccao.javet.swc4j.ast.Swc4jAst;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstAssignProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClass;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassMethod;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstComputedPropName;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstKeyValueProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstMethodProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstParam;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstArrowExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstCallExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstExprOrSpread;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstFnExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdentName;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxElement;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxFragment;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstMemberExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstSpreadElement;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstTpl;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstArrayLit;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstBool;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstNull;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstNumber;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstObjectLit;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstStr;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstExpr;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstPat;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstPropName;
import com.caoccao.javet.swc4j.ast.module.Swc4jAstExportDefaultExpr;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstArrayPat;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstAssignPatProp;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstKeyValuePatProp;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstObjectPat;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstRestPat;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstClassDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDeclarator;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import org.example.sourcecodeanalyser.model.frontend.ImportExportAnalysis;
import org.example.sourcecodeanalyser.model.frontend.ReactAnalysis;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ReactAnalyzer {

    private static final int MAX_EXPRESSION_DEPTH = 3;
    private static final int MAX_ARRAY_ELEMENTS = 10;

    private static final Set<String> BUILT_IN_HOOKS = Set.of(
            "useState", "useEffect", "useContext", "useReducer",
            "useCallback", "useMemo", "useRef", "useImperativeHandle",
            "useLayoutEffect", "useDebugValue", "useId", "useTransition",
            "useDeferredValue", "useSyncExternalStore", "useInsertionEffect",
            "useActionState", "useOptimistic"
    );

    private static final Set<String> REACT_CLASS_BASES = Set.of(
            "Component", "PureComponent"
    );

    private static final Set<String> AXIOS_HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options"
    );

    private final ImportExportAnalyzer importExportAnalyzer;

    public ReactAnalyzer(ImportExportAnalyzer importExportAnalyzer) {
        this.importExportAnalyzer = importExportAnalyzer;
    }

    public ReactAnalysis analyze(Swc4jParseOutput parseOutput) {

        if (parseOutput == null || parseOutput.getProgram() == null) {
            return new ReactAnalysis(List.of(), List.of(), List.of());
        }

        ISwc4jAst program = parseOutput.getProgram();

        List<DetectedComponent> detectedComponents = new ArrayList<>();

        findFunctionComponents(program, detectedComponents);
        findVariableComponents(program, detectedComponents);
        findClassComponents(program, detectedComponents);

        Set<String> exportedNames = findExportedNames(parseOutput);
        Set<String> defaultExportNames = findDefaultExportNames(program);

        List<ReactAnalysis.ComponentInfo> components = new ArrayList<>();

        for (DetectedComponent component : detectedComponents) {

            boolean defaultExport = defaultExportNames.contains(component.name());
            boolean exported = exportedNames.contains(component.name()) || defaultExport;

            components.add(new ReactAnalysis.ComponentInfo(
                    component.name(),
                    component.kind(),
                    exported,
                    defaultExport,
                    findProps(component),
                    findState(component)
            ));
        }

        List<ReactAnalysis.HookInfo> hooks = findHooks(detectedComponents);
        List<ReactAnalysis.ApiInteractionInfo> apiInteractions =
                findApiInteractions(detectedComponents);
        List<ReactAnalysis.AxiosInstanceInfo> axiosInstances =
                findAxiosInstances(program);
        List<ReactAnalysis.CustomHookInfo> customHooks =
                findCustomHooks(program, detectedComponents, exportedNames);
        List<ReactAnalysis.RouteInfo> routes = findRoutes(program);
        List<ReactAnalysis.NavigationInfo> navigations =
                findNavigations(detectedComponents);
        List<ReactAnalysis.RouterHookInfo> routerHooks =
                findRouterHooks(detectedComponents);
        List<ReactAnalysis.FunctionInfo> functions =
                findFunctions(program, detectedComponents);

        return new ReactAnalysis(
                List.copyOf(components),
                List.copyOf(hooks),
                List.copyOf(apiInteractions),
                List.copyOf(axiosInstances),
                List.copyOf(customHooks),
                List.copyOf(routes),
                List.copyOf(navigations),
                List.copyOf(routerHooks),
                List.copyOf(functions)
        );
    }

    // ---------------------------------------------------------
    // React component detection
    // ---------------------------------------------------------

    private void findFunctionComponents(
            ISwc4jAst program,
            List<DetectedComponent> components) {

        for (Swc4jAstFnDecl declaration : program.find(Swc4jAstFnDecl.class)) {

            if (declaration.getIdent() == null) {
                continue;
            }

            String name = declaration.getIdent().getSym();

            if (!isComponentName(name) || !containsJsx(declaration)) {
                continue;
            }

            components.add(new DetectedComponent(
                    name, "FUNCTION_DECLARATION", declaration
            ));
        }
    }

    private void findVariableComponents(
            ISwc4jAst program,
            List<DetectedComponent> components) {

        for (Swc4jAstVarDecl declaration : program.find(Swc4jAstVarDecl.class)) {

            for (Swc4jAstVarDeclarator declarator : declaration.getDecls()) {

                String name = extractIdentifier(declarator.getName());

                if (!isComponentName(name)) {
                    continue;
                }

                Optional<ISwc4jAstExpr> initializer = declarator.getInit();

                if (initializer.isEmpty()) {
                    continue;
                }

                ISwc4jAstExpr expression = unwrapComponentExpression(initializer.get());

                String kind;

                if (expression instanceof Swc4jAstArrowExpr) {
                    kind = "ARROW";
                } else if (expression instanceof Swc4jAstFnExpr) {
                    kind = "FUNCTION_EXPRESSION";
                } else {
                    continue;
                }

                if (!containsJsx(expression)) {
                    continue;
                }

                components.add(new DetectedComponent(name, kind, expression));
            }
        }
    }

    private void findClassComponents(
            ISwc4jAst program,
            List<DetectedComponent> components) {

        for (Swc4jAstClassDecl declaration : program.find(Swc4jAstClassDecl.class)) {

            if (declaration.getIdent() == null) {
                continue;
            }

            String name = declaration.getIdent().getSym();

            if (!isComponentName(name) || !isReactClassComponent(declaration)) {
                continue;
            }

            components.add(new DetectedComponent(name, "CLASS", declaration));
        }
    }

    private boolean isReactClassComponent(Swc4jAstClassDecl declaration) {

        Optional<ISwc4jAstExpr> superClass =
                declaration.getClazz().getSuperClass();

        if (superClass.isEmpty()) {
            return false;
        }

        String superClassName = extractLastIdentifier(superClass.get());

        return REACT_CLASS_BASES.contains(superClassName)
                && containsJsx(declaration.getClazz());
    }

    private String extractLastIdentifier(ISwc4jAst node) {

        if (node instanceof Swc4jAstIdent identifier) {
            return identifier.getSym();
        }

        if (node instanceof Swc4jAstMemberExpr member
                && member.getProp() instanceof Swc4jAstIdentName property) {
            return property.getSym();
        }

        List<Swc4jAstIdent> identifiers = node.find(Swc4jAstIdent.class);

        if (identifiers.isEmpty()) {
            return null;
        }

        return identifiers.get(identifiers.size() - 1).getSym();
    }

    private String extractIdentifier(ISwc4jAst node) {

        if (node instanceof Swc4jAstIdent identifier) {
            return identifier.getSym();
        }

        List<Swc4jAstIdent> identifiers = node.find(Swc4jAstIdent.class);

        return identifiers.isEmpty() ? null : identifiers.get(0).getSym();
    }

    private boolean containsJsx(ISwc4jAst node) {
        return !node.find(Swc4jAstJsxElement.class).isEmpty()
                || !node.find(Swc4jAstJsxFragment.class).isEmpty();
    }

    // ---------------------------------------------------------
    // Export detection
    // ---------------------------------------------------------

    private Set<String> findExportedNames(Swc4jParseOutput parseOutput) {

        Set<String> names = new HashSet<>();

        ImportExportAnalysis analysis = importExportAnalyzer.analyze(parseOutput);

        for (ImportExportAnalysis.ExportInfo export : analysis.exports()) {
            if (export.names() != null) {
                names.addAll(export.names());
            }
        }

        return names;
    }

    private Set<String> findDefaultExportNames(ISwc4jAst program) {

        Set<String> names = new HashSet<>();

        for (Swc4jAstExportDefaultExpr export
                : program.find(Swc4jAstExportDefaultExpr.class)) {

            if (export.getExpr() instanceof Swc4jAstIdent identifier) {
                names.add(identifier.getSym());
            }
        }

        return names;
    }

    // ---------------------------------------------------------
    // Props extraction
    // ---------------------------------------------------------

    private List<String> findProps(DetectedComponent component) {

        if ("CLASS".equals(component.kind())) {
            return List.of();
        }

        List<ISwc4jAstPat> parameters = findComponentParameters(component);

        if (parameters.isEmpty()) {
            return List.of();
        }

        ISwc4jAstPat first = parameters.get(0);

        if (first instanceof Swc4jAstBindingIdent binding) {
            return List.of(binding.getId().getSym());
        }

        if (first instanceof Swc4jAstObjectPat objectPattern) {

            List<String> props = new ArrayList<>();

            for (var property : objectPattern.getProps()) {

                if (property instanceof Swc4jAstKeyValuePatProp keyValue) {

                    String name = extractPropName(keyValue.getKey());

                    if (name != null) {
                        props.add(name);
                    }

                } else if (property instanceof Swc4jAstAssignPatProp assign) {

                    props.add(assign.getKey().getId().getSym());

                } else if (property instanceof Swc4jAstRestPat rest) {

                    String name = extractBindingName(rest.getArg());

                    if (name != null) {
                        props.add("..." + name);
                    }
                }
            }

            return List.copyOf(props);
        }

        return List.of();
    }

    private List<ISwc4jAstPat> findComponentParameters(DetectedComponent component) {

        ISwc4jAst node = component.node();

        if (node instanceof Swc4jAstFnDecl declaration) {
            return declaration.getFunction().getParams()
                    .stream().map(Swc4jAstParam::getPat).toList();
        }

        if (node instanceof Swc4jAstArrowExpr arrow) {
            return arrow.getParams();
        }

        if (node instanceof Swc4jAstFnExpr function) {
            return function.getFunction().getParams()
                    .stream().map(Swc4jAstParam::getPat).toList();
        }

        return List.of();
    }

    private String extractPropName(ISwc4jAstPropName name) {

        if (name instanceof Swc4jAstIdentName identifier) {
            return identifier.getSym();
        }

        if (name instanceof Swc4jAstStr string) {
            return string.getValue();
        }

        if (name instanceof Swc4jAstNumber number) {
            return number.getRaw().orElse(Double.toString(number.getValue()));
        }

        return null;
    }

    private String extractBindingName(ISwc4jAstPat pattern) {

        if (pattern instanceof Swc4jAstBindingIdent binding) {
            return binding.getId().getSym();
        }

        return null;
    }

    // ---------------------------------------------------------
    // useState extraction
    // ---------------------------------------------------------

    private List<ReactAnalysis.StateInfo> findState(DetectedComponent component) {

        if ("CLASS".equals(component.kind())) {
            return List.of();
        }

        List<ReactAnalysis.StateInfo> states = new ArrayList<>();

        for (Swc4jAstVarDeclarator declarator
                : component.node().find(Swc4jAstVarDeclarator.class)) {

            if (!(declarator.getName() instanceof Swc4jAstArrayPat array)) {
                continue;
            }

            if (array.getElems().size() < 2) {
                continue;
            }

            Optional<ISwc4jAstExpr> initializer = declarator.getInit();

            if (initializer.isEmpty()
                    || !(initializer.get() instanceof Swc4jAstCallExpr call)
                    || !"useState".equals(findCalleeName(call))) {
                continue;
            }

            Optional<ISwc4jAstPat> statePattern = array.getElems().get(0);
            Optional<ISwc4jAstPat> setterPattern = array.getElems().get(1);

            if (statePattern.isEmpty() || setterPattern.isEmpty()) {
                continue;
            }

            String stateName = extractBindingName(statePattern.get());
            String setterName = extractBindingName(setterPattern.get());

            if (stateName == null || setterName == null) {
                continue;
            }

            String initialValue = call.getArgs().isEmpty()
                    ? "<no initial value>"
                    : formatExpression(call.getArgs().get(0).getExpr(), 0);

            states.add(new ReactAnalysis.StateInfo(
                    stateName, setterName, "useState", initialValue
            ));
        }

        return List.copyOf(states);
    }

    // ---------------------------------------------------------
    // AST call collection
    // ---------------------------------------------------------

    /*
     * IMPORTANT:
     * Do not use getParent() here.
     *
     * Parent links are not reliably initialized in the parsed AST.
     * ISwc4jAst.find() already traverses the component subtree.
     *
     * This implementation restores API and hook extraction.
     */

    private List<OwnedCall> findOwnedCalls(DetectedComponent component) {

        List<OwnedCall> calls = new ArrayList<>();

        collectOwnedCalls(
                component.node(),
                component.name(),
                "COMPONENT",
                true,
                calls
        );

        return List.copyOf(calls);
    }

    private void collectOwnedCalls(
            ISwc4jAst node,
            String ownerName,
            String ownerKind,
            boolean componentRoot,
            List<OwnedCall> calls) {

        if (node == null) {
            return;
        }

        String currentOwnerName = ownerName;
        String currentOwnerKind = ownerKind;

        // Do not treat the component's own function as a nested function.
        if (!componentRoot) {

            // Named nested function declaration.
            if (node instanceof Swc4jAstFnDecl function) {

                if (function.getIdent() != null) {
                    currentOwnerName = function.getIdent().getSym();
                } else {
                    currentOwnerName = "<anonymous function>";
                }

                currentOwnerKind = "FUNCTION";
            }

            // Nested arrow function.
            else if (node instanceof Swc4jAstArrowExpr) {

                currentOwnerName = "<anonymous function>";
                currentOwnerKind = "ARROW_FUNCTION";
            }

            // Nested function expression.
            else if (node instanceof Swc4jAstFnExpr) {

                currentOwnerName = "<anonymous function>";
                currentOwnerKind = "FUNCTION_EXPRESSION";
            }

            // Class method.
            else if (node instanceof Swc4jAstClassMethod method) {

                String methodName = extractPropName(method.getKey());

                currentOwnerName = methodName == null
                        ? "<computed method>"
                        : methodName;

                currentOwnerKind = "CLASS_METHOD";
            }

            // Do not attribute calls from an unrelated nested class
            // to the current component.
            else if (node instanceof Swc4jAstClassDecl) {
                return;
            }
        }

        // Record a call using its nearest enclosing function owner.
        if (node instanceof Swc4jAstCallExpr call) {

            calls.add(
                    new OwnedCall(
                            call,
                            currentOwnerName,
                            currentOwnerKind
                    )
            );
        }

        // Traverse the AST children while carrying the current owner.
        if (node instanceof Swc4jAst ast) {

            for (ISwc4jAst child : ast.getChildNodes()) {

                collectOwnedCalls(
                        child,
                        currentOwnerName,
                        currentOwnerKind,
                        false,
                        calls
                );
            }
        }
    }


    // ---------------------------------------------------------
    // API interaction extraction
    // ---------------------------------------------------------

    private List<ReactAnalysis.ApiInteractionInfo> findApiInteractions(
            List<DetectedComponent> components) {

        List<ReactAnalysis.ApiInteractionInfo> interactions = new ArrayList<>();

        for (DetectedComponent component : components) {

            for (OwnedCall ownedCall : findOwnedCalls(component)) {

                Swc4jAstCallExpr call = ownedCall.call();
                var callee = call.getCallee();

                String client = null;
                String method = null;
                String endpoint = "<unknown endpoint>";
                Swc4jAstObjectLit requestConfig = null;
                Map<String, String> headers = Map.of();
                Map<String, String> queryParams = new LinkedHashMap<>();
                String body = null;

                if (isFetchCall(call)) {
                    client = "fetch";
                    method = "GET";
                    if (!call.getArgs().isEmpty()) {
                        endpoint = extractApiEndpoint(call.getArgs().get(0).getExpr());
                    }
                    if (call.getArgs().size() > 1
                            && call.getArgs().get(1).getExpr() instanceof Swc4jAstObjectLit options) {
                        requestConfig = options;
                        Optional<ISwc4jAstExpr> methodValue = findObjectPropertyValue(options, "method");
                        if (methodValue.isPresent()) method = extractHttpMethod(methodValue.get());
                        headers = extractObjectMap(findObjectPropertyValue(options, "headers").orElse(null));
                        body = findObjectPropertyValue(options, "body")
                                .map(value -> formatExpression(value, 0)).orElse(null);
                    }
                } else if (callee instanceof Swc4jAstMemberExpr member
                        && member.getProp() instanceof Swc4jAstIdentName property) {
                    String memberName = property.getSym().toLowerCase();
                    if (AXIOS_HTTP_METHODS.contains(memberName)) {
                        client = formatExpression(member.getObj(), 0);
                        method = memberName.toUpperCase();
                        if (!call.getArgs().isEmpty()) endpoint = extractApiEndpoint(call.getArgs().get(0).getExpr());
                        if (call.getArgs().size() > 1
                                && call.getArgs().get(1).getExpr() instanceof Swc4jAstObjectLit config) {
                            requestConfig = config;
                        }
                        if (Set.of("post", "put", "patch").contains(memberName) && call.getArgs().size() > 1) {
                            body = formatExpression(call.getArgs().get(1).getExpr(), 0);
                        }
                        if (requestConfig != null) {
                            headers = extractObjectMap(findObjectPropertyValue(requestConfig, "headers").orElse(null));
                            queryParams.putAll(extractObjectMap(findObjectPropertyValue(requestConfig, "params").orElse(null)));
                            if (body == null) body = findObjectPropertyValue(requestConfig, "data")
                                    .map(value -> formatExpression(value, 0)).orElse(null);
                        }
                    }
                } else if (callee instanceof Swc4jAstIdent identifier
                        && !call.getArgs().isEmpty()
                        && call.getArgs().get(0).getExpr() instanceof Swc4jAstObjectLit config
                        && isAxiosConfigCall(identifier.getSym())) {
                    client = identifier.getSym();
                    requestConfig = config;
                    method = findObjectPropertyValue(config, "method")
                            .map(this::extractHttpMethod).orElse("GET");
                    endpoint = findObjectPropertyValue(config, "url")
                            .map(this::extractApiEndpoint).orElse(endpoint);
                    headers = extractObjectMap(findObjectPropertyValue(config, "headers").orElse(null));
                    queryParams.putAll(extractObjectMap(findObjectPropertyValue(config, "params").orElse(null)));
                    body = findObjectPropertyValue(config, "data")
                            .map(value -> formatExpression(value, 0)).orElse(null);
                }

                if (client == null || method == null) continue;

                List<String> arguments = call.getArgs().stream()
                        .map(argument -> formatExpression(argument.getExpr(), 0)).toList();
                queryParams.putAll(extractQueryString(endpoint));

                interactions.add(new ReactAnalysis.ApiInteractionInfo(
                        component.name(), client, method, endpoint, arguments,
                        ownedCall.ownerName(), ownedCall.ownerKind(),
                        Map.copyOf(headers), Map.copyOf(queryParams), body
                ));
            }
        }

        return List.copyOf(interactions);
    }

    private Optional<ISwc4jAstExpr> findObjectPropertyValue(
            Swc4jAstObjectLit object,
            String propertyName) {

        for (var property : object.getProps()) {

            if (!(property instanceof Swc4jAstKeyValueProp keyValue)) {
                continue;
            }

            String key = extractPropName(keyValue.getKey());

            if (propertyName.equals(key)) {
                return Optional.of(keyValue.getValue());
            }
        }

        return Optional.empty();
    }

    private String extractHttpMethod(ISwc4jAstExpr expression) {

        if (expression instanceof Swc4jAstStr string) {
            return string.getValue().toUpperCase();
        }

        return formatExpression(expression, 0);
    }

    private String extractApiEndpoint(ISwc4jAstExpr expression) {

        if (expression instanceof Swc4jAstStr string) {
            return string.getValue();
        }

        return formatExpression(expression, 0);
    }

    // ---------------------------------------------------------
    // Hook extraction
    // ---------------------------------------------------------

    private List<ReactAnalysis.HookInfo> findHooks(
            List<DetectedComponent> components) {

        List<ReactAnalysis.HookInfo> hooks = new ArrayList<>();

        for (DetectedComponent component : components) {

            if ("CLASS".equals(component.kind())) {
                continue;
            }

            for (OwnedCall ownedCall : findOwnedCalls(component)) {

                Swc4jAstCallExpr call = ownedCall.call();
                String hookName = findCalleeName(call);

                if (!isHookName(hookName)) {
                    continue;
                }

                List<String> arguments = call.getArgs().stream()
                        .map(argument -> formatExpression(argument.getExpr(), 0))
                        .toList();

                boolean custom = !BUILT_IN_HOOKS.contains(hookName);

                hooks.add(new ReactAnalysis.HookInfo(
                        hookName,
                        component.name(),
                        arguments,
                        custom,
                        ownedCall.ownerName(),
                        ownedCall.ownerKind()
                ));
            }
        }

        return List.copyOf(hooks);
    }

    // ---------------------------------------------------------
    // Axios instances, custom hooks, routing, navigation and functions
    // ---------------------------------------------------------

    private ISwc4jAstExpr unwrapComponentExpression(ISwc4jAstExpr expression) {
        ISwc4jAstExpr current = expression;
        // Supports React.memo(Component), memo(Component), and forwardRef((props, ref) => ...).
        for (int i = 0; i < 4 && current instanceof Swc4jAstCallExpr call; i++) {
            String callee = findCalleeName(call);
            if (callee == null || !Set.of("memo", "forwardRef", "lazy").contains(callee)
                    || call.getArgs().isEmpty()) break;
            current = call.getArgs().get(0).getExpr();
        }
        return current;
    }

    private List<ReactAnalysis.AxiosInstanceInfo> findAxiosInstances(ISwc4jAst program) {
        List<ReactAnalysis.AxiosInstanceInfo> result = new ArrayList<>();
        for (Swc4jAstVarDecl declaration : program.find(Swc4jAstVarDecl.class)) {
            for (Swc4jAstVarDeclarator declarator : declaration.getDecls()) {
                String variableName = extractIdentifier(declarator.getName());
                Optional<ISwc4jAstExpr> init = declarator.getInit();
                if (variableName == null || init.isEmpty() || !(init.get() instanceof Swc4jAstCallExpr call)) continue;
                if (!isAxiosCreateCall(call)) continue;
                if (call.getArgs().isEmpty() || !(call.getArgs().get(0).getExpr() instanceof Swc4jAstObjectLit config)) continue;
                String baseUrl = findObjectPropertyValue(config, "baseURL")
                        .map(this::extractApiEndpoint).orElse(null);
                Map<String, String> headers = extractObjectMap(findObjectPropertyValue(config, "headers").orElse(null));
                String timeout = findObjectPropertyValue(config, "timeout")
                        .map(value -> formatExpression(value, 0)).orElse(null);
                result.add(new ReactAnalysis.AxiosInstanceInfo(variableName, "axios.create", baseUrl, headers, timeout));
            }
        }
        return List.copyOf(result);
    }

    private boolean isAxiosCreateCall(Swc4jAstCallExpr call) {
        if (!(call.getCallee() instanceof Swc4jAstMemberExpr member)
                || !(member.getProp() instanceof Swc4jAstIdentName property)
                || !"create".equals(property.getSym())) return false;
        return member.getObj() instanceof Swc4jAstIdent id && "axios".equals(id.getSym());
    }

    private boolean isFetchCall(Swc4jAstCallExpr call) {
        return call.getCallee() instanceof Swc4jAstIdent id && "fetch".equals(id.getSym());
    }

    private boolean isAxiosConfigCall(String name) {
        return "axios".equals(name);
    }

    private Map<String, String> extractObjectMap(ISwc4jAstExpr expression) {
        if (!(expression instanceof Swc4jAstObjectLit object)) return Map.of();
        Map<String, String> values = new LinkedHashMap<>();
        for (var property : object.getProps()) {
            if (property instanceof Swc4jAstKeyValueProp pair) {
                String key = extractPropName(pair.getKey());
                if (key != null) values.put(key, formatExpression(pair.getValue(), 0));
            } else if (property instanceof Swc4jAstAssignProp assign) {
                values.put(assign.getKey().getSym(), formatExpression(assign.getValue(), 0));
            } else if (property instanceof Swc4jAstSpreadElement spread) {
                values.put("...", formatExpression(spread.getExpr(), 0));
            }
        }
        return Map.copyOf(values);
    }

    private Map<String, String> extractQueryString(String endpoint) {
        Map<String, String> result = new LinkedHashMap<>();
        if (endpoint == null) return result;
        int queryStart = endpoint.indexOf('?');
        if (queryStart < 0 || queryStart == endpoint.length() - 1) return result;
        String query = endpoint.substring(queryStart + 1).split("#", 2)[0];
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            String[] parts = pair.split("=", 2);
            result.put(parts[0], parts.length > 1 ? parts[1] : "");
        }
        return result;
    }

    private List<ReactAnalysis.CustomHookInfo> findCustomHooks(
            ISwc4jAst program, List<DetectedComponent> components, Set<String> exportedNames) {
        Map<String, HookDefinition> definitions = new LinkedHashMap<>();
        for (Swc4jAstFnDecl declaration : program.find(Swc4jAstFnDecl.class)) {
            if (declaration.getIdent() == null) continue;
            String name = declaration.getIdent().getSym();
            if (isCustomHookName(name)) {
                definitions.put(name, new HookDefinition(name, declaration,
                        declaration.getFunction().getParams().stream().map(p -> formatPattern(p.getPat())).toList(),
                        exportedNames.contains(name)));
            }
        }
        for (Swc4jAstVarDecl declaration : program.find(Swc4jAstVarDecl.class)) {
            for (Swc4jAstVarDeclarator declarator : declaration.getDecls()) {
                String name = extractIdentifier(declarator.getName());
                Optional<ISwc4jAstExpr> init = declarator.getInit();
                if (!isCustomHookName(name) || init.isEmpty()) continue;
                ISwc4jAstExpr function = unwrapComponentExpression(init.get());
                if (function instanceof Swc4jAstArrowExpr arrow) {
                    definitions.put(name, new HookDefinition(name, arrow,
                            arrow.getParams().stream().map(this::formatPattern).toList(), exportedNames.contains(name)));
                } else if (function instanceof Swc4jAstFnExpr fn) {
                    definitions.put(name, new HookDefinition(name, fn,
                            fn.getFunction().getParams().stream().map(p -> formatPattern(p.getPat())).toList(), exportedNames.contains(name)));
                }
            }
        }
        List<ReactAnalysis.CustomHookInfo> result = new ArrayList<>();
        for (HookDefinition definition : definitions.values()) {
            LinkedHashSet<String> usedHooks = new LinkedHashSet<>();
            for (Swc4jAstCallExpr call : definition.node().find(Swc4jAstCallExpr.class)) {
                String called = findCalleeName(call);
                if (isHookName(called)) usedHooks.add(called);
            }
            LinkedHashSet<String> users = new LinkedHashSet<>();
            for (DetectedComponent component : components) {
                for (OwnedCall owned : findOwnedCalls(component)) {
                    if (definition.name().equals(findCalleeName(owned.call()))) users.add(component.name());
                }
            }
            result.add(new ReactAnalysis.CustomHookInfo(definition.name(), definition.parameters(),
                    definition.exported(), List.copyOf(usedHooks), List.copyOf(users)));
        }
        return List.copyOf(result);
    }

    private boolean isCustomHookName(String name) {
        return name != null && name.matches("use[A-Z0-9].*") && !BUILT_IN_HOOKS.contains(name);
    }

    private String formatPattern(ISwc4jAstPat pattern) {
        if (pattern instanceof Swc4jAstBindingIdent binding) return binding.getId().getSym();
        if (pattern instanceof Swc4jAstObjectPat object) {
            List<String> names = new ArrayList<>();
            for (var property : object.getProps()) {
                if (property instanceof Swc4jAstKeyValuePatProp pair) {
                    String key = extractPropName(pair.getKey()); if (key != null) names.add(key);
                } else if (property instanceof Swc4jAstAssignPatProp assign) names.add(assign.getKey().getId().getSym());
                else if (property instanceof Swc4jAstRestPat rest) names.add("..." + extractBindingName(rest.getArg()));
            }
            return "{ " + String.join(", ", names) + " }";
        }
        return pattern.getClass().getSimpleName();
    }

    private List<ReactAnalysis.RouteInfo> findRoutes(ISwc4jAst program) {
        List<ReactAnalysis.RouteInfo> routes = new ArrayList<>();
        for (Swc4jAstCallExpr call : program.find(Swc4jAstCallExpr.class)) {
            String callee = findCalleeName(call);
            if (callee == null || !Set.of("createBrowserRouter", "createHashRouter", "createMemoryRouter").contains(callee)
                    || call.getArgs().isEmpty()) continue;
            ISwc4jAstExpr config = call.getArgs().get(0).getExpr();
            if (config instanceof Swc4jAstArrayLit array) collectRouteArray(array, null, routes);
        }
        return List.copyOf(routes);
    }

    private void collectRouteArray(Swc4jAstArrayLit array, String parentPath,
                                   List<ReactAnalysis.RouteInfo> routes) {
        for (Optional<Swc4jAstExprOrSpread> item : array.getElems()) {
            if (item.isEmpty() || !(item.get().getExpr() instanceof Swc4jAstObjectLit route)) continue;
            String path = findObjectPropertyValue(route, "path").map(this::extractApiEndpoint).orElse(null);
            String element = findObjectPropertyValue(route, "element").map(value -> formatExpression(value, 0)).orElse(null);
            String component = findObjectPropertyValue(route, "Component").map(value -> formatExpression(value, 0)).orElse(null);
            String index = findObjectPropertyValue(route, "index").map(value -> formatExpression(value, 0)).orElse(null);
            String routeType = index != null && "true".equals(index) ? "INDEX" : "OBJECT_ROUTE";
            routes.add(new ReactAnalysis.RouteInfo(path, component, element, parentPath, routeType));
            Optional<ISwc4jAstExpr> children = findObjectPropertyValue(route, "children");
            if (children.isPresent() && children.get() instanceof Swc4jAstArrayLit nested) {
                collectRouteArray(nested, path == null ? parentPath : path, routes);
            }
        }
    }

    private List<ReactAnalysis.NavigationInfo> findNavigations(List<DetectedComponent> components) {
        List<ReactAnalysis.NavigationInfo> result = new ArrayList<>();
        for (DetectedComponent component : components) {
            for (OwnedCall owned : findOwnedCalls(component)) {
                Swc4jAstCallExpr call = owned.call();
                String name = findCalleeName(call);
                String method = null;
                String destination = null;
                boolean navigationCall = "navigate".equals(name)
                        || isHistoryOrLocationMethod(call, name);
                if (navigationCall) {
                    method = name;
                    if (!call.getArgs().isEmpty()) destination = formatExpression(call.getArgs().get(0).getExpr(), 0);
                }
                if (method != null) {
                    result.add(new ReactAnalysis.NavigationInfo(component.name(), owned.ownerName(),
                            owned.ownerKind(), method, destination,
                            call.getArgs().stream().map(arg -> formatExpression(arg.getExpr(), 0)).toList()));
                }
            }
        }
        return List.copyOf(result);
    }

    private boolean isHistoryOrLocationMethod(Swc4jAstCallExpr call, String method) {
        if (!(call.getCallee() instanceof Swc4jAstMemberExpr member)) return false;
        String receiver = formatExpression(member.getObj(), 0);
        if (("push".equals(method) || "replace".equals(method) || "go".equals(method))
                && ("history".equals(receiver) || receiver.endsWith(".history"))) return true;
        return ("assign".equals(method) || "replace".equals(method))
                && (receiver.contains("location") || receiver.contains("window.location"));
    }

    private List<ReactAnalysis.RouterHookInfo> findRouterHooks(List<DetectedComponent> components) {
        Set<String> routerHooks = Set.of("useNavigate", "useParams", "useLocation", "useSearchParams",
                "useMatch", "useResolvedPath", "useNavigation", "useRouteLoaderData", "useLoaderData",
                "useActionData", "useSubmit", "useFetcher", "useRouteError", "useOutlet");
        List<ReactAnalysis.RouterHookInfo> result = new ArrayList<>();
        for (DetectedComponent component : components) {
            for (OwnedCall owned : findOwnedCalls(component)) {
                String name = findCalleeName(owned.call());
                if (name != null && routerHooks.contains(name)) {
                    result.add(new ReactAnalysis.RouterHookInfo(name, component.name(), owned.ownerName(),
                            owned.ownerKind(), owned.call().getArgs().stream()
                            .map(arg -> formatExpression(arg.getExpr(), 0)).toList()));
                }
            }
        }
        return List.copyOf(result);
    }

    private List<ReactAnalysis.FunctionInfo> findFunctions(ISwc4jAst program,
                                                            List<DetectedComponent> components) {
        List<ReactAnalysis.FunctionInfo> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Swc4jAstFnDecl declaration : program.find(Swc4jAstFnDecl.class)) {
            if (declaration.getIdent() == null) continue;
            String name = declaration.getIdent().getSym();
            String key = name + "|FUNCTION_DECLARATION";
            if (seen.add(key)) result.add(new ReactAnalysis.FunctionInfo(name, null,
                    "FUNCTION_DECLARATION", declaration.getFunction().getParams().stream()
                    .map(p -> formatPattern(p.getPat())).toList(), findContainingComponent(name, components)));
        }
        for (Swc4jAstVarDecl declaration : program.find(Swc4jAstVarDecl.class)) {
            for (Swc4jAstVarDeclarator declarator : declaration.getDecls()) {
                String name = extractIdentifier(declarator.getName());
                Optional<ISwc4jAstExpr> init = declarator.getInit();
                if (name == null || init.isEmpty()) continue;
                ISwc4jAstExpr expression = init.get();
                List<String> params;
                String kind;
                if (expression instanceof Swc4jAstArrowExpr arrow) {
                    params = arrow.getParams().stream().map(this::formatPattern).toList(); kind = "ARROW";
                } else if (expression instanceof Swc4jAstFnExpr fn) {
                    params = fn.getFunction().getParams().stream().map(p -> formatPattern(p.getPat())).toList(); kind = "FUNCTION_EXPRESSION";
                } else continue;
                String key = name + "|" + kind;
                if (seen.add(key)) result.add(new ReactAnalysis.FunctionInfo(name, null, kind, params,
                        findContainingComponent(name, components)));
            }
        }
        for (DetectedComponent component : components) {
            for (OwnedCall owned : findOwnedCalls(component)) {
                if (!component.name().equals(owned.ownerName()) && !owned.ownerName().startsWith("<")) {
                    String key = owned.ownerName() + "|" + owned.ownerKind();
                    if (seen.add(key)) result.add(new ReactAnalysis.FunctionInfo(owned.ownerName(),
                            component.name(), owned.ownerKind(), List.of(), component.name()));
                }
            }
        }
        return List.copyOf(result);
    }

    private String findContainingComponent(String functionName, List<DetectedComponent> components) {
        // Top-level functions are not reliably nested under a component in the AST;
        // component association is therefore left null unless the function itself is a component.
        for (DetectedComponent component : components) if (component.name().equals(functionName)) return component.name();
        return null;
    }

    // ---------------------------------------------------------
    // Expression formatting
    // ---------------------------------------------------------

    private String formatExpression(ISwc4jAstExpr expression, int depth) {

        if (expression == null) {
            return "<expression>";
        }

        if (depth > MAX_EXPRESSION_DEPTH) {
            return "<nested expression>";
        }

        if (expression instanceof Swc4jAstIdent identifier) {
            return identifier.getSym();
        }

        if (expression instanceof Swc4jAstIdentName identifier) {
            return identifier.getSym();
        }

        if (expression instanceof Swc4jAstStr string) {
            return quote(string.getValue());
        }

        if (expression instanceof Swc4jAstTpl template) {
            return formatTemplateLiteral(template, depth);
        }

        if (expression instanceof Swc4jAstNumber number) {
            return number.getRaw().orElse(Double.toString(number.getValue()));
        }

        if (expression instanceof Swc4jAstBool bool) {
            return Boolean.toString(bool.asBoolean());
        }

        if (expression instanceof Swc4jAstNull) {
            return "null";
        }

        if (expression instanceof Swc4jAstMemberExpr member) {
            return formatMemberExpression(member, depth);
        }

        if (expression instanceof Swc4jAstArrayLit array) {
            return formatArrayExpression(array, depth);
        }

        if (expression instanceof Swc4jAstObjectLit object) {
            return formatObjectExpression(object, depth);
        }

        if (expression instanceof Swc4jAstArrowExpr
                || expression instanceof Swc4jAstFnExpr) {
            return "<callback>";
        }

        return "<expression>";
    }

    private String formatTemplateLiteral(Swc4jAstTpl template, int depth) {

        StringBuilder result = new StringBuilder();

        var quasis = template.getQuasis();
        var expressions = template.getExprs();

        for (int i = 0; i < quasis.size(); i++) {

            result.append(quasis.get(i).getRaw());

            if (i < expressions.size()) {
                result.append("${")
                        .append(formatExpression(expressions.get(i), depth + 1))
                        .append("}");
            }
        }

        return result.toString();
    }

    private String formatMemberExpression(
            Swc4jAstMemberExpr member,
            int depth) {

        String object = formatExpression(member.getObj(), depth + 1);
        var property = member.getProp();

        if (property instanceof Swc4jAstIdentName identifier) {
            return object + "." + identifier.getSym();
        }

        if (property instanceof Swc4jAstComputedPropName computed) {
            return object + "["
                    + formatExpression(computed.getExpr(), depth + 1)
                    + "]";
        }

        return "<member expression>";
    }

    private String formatArrayExpression(Swc4jAstArrayLit array, int depth) {

        List<String> elements = new ArrayList<>();

        for (Optional<Swc4jAstExprOrSpread> optional : array.getElems()) {

            if (elements.size() >= MAX_ARRAY_ELEMENTS) {
                elements.add("...");
                break;
            }

            if (optional.isEmpty()) {
                elements.add("");
                continue;
            }

            elements.add(formatExpression(optional.get().getExpr(), depth + 1));
        }

        return "[" + String.join(", ", elements) + "]";
    }

    private String formatObjectExpression(Swc4jAstObjectLit object, int depth) {

        List<String> properties = new ArrayList<>();

        for (var property : object.getProps()) {

            if (properties.size() >= MAX_ARRAY_ELEMENTS) {
                properties.add("...");
                break;
            }

            if (property instanceof Swc4jAstKeyValueProp keyValue) {

                String key = formatPropertyName(keyValue.getKey(), depth);
                String value = formatExpression(keyValue.getValue(), depth + 1);

                properties.add(key + ": " + value);

            } else if (property instanceof Swc4jAstAssignProp assign) {

                String key = assign.getKey().getSym();
                String value = formatExpression(assign.getValue(), depth + 1);

                properties.add(key + " = " + value);

            } else if (property instanceof Swc4jAstSpreadElement spread) {

                properties.add("..." + formatExpression(
                        spread.getExpr(), depth + 1
                ));

            } else if (property instanceof Swc4jAstMethodProp method) {

                properties.add(formatPropertyName(method.getKey(), depth)
                        + ": <method>");

            } else {
                properties.add("<property>");
            }
        }

        return "{ " + String.join(", ", properties) + " }";
    }

    private String formatPropertyName(ISwc4jAstPropName name, int depth) {

        if (name instanceof Swc4jAstIdentName identifier) {
            return identifier.getSym();
        }

        if (name instanceof Swc4jAstStr string) {
            return quote(string.getValue());
        }

        if (name instanceof Swc4jAstNumber number) {
            return number.getRaw().orElse(Double.toString(number.getValue()));
        }

        if (name instanceof Swc4jAstComputedPropName computed) {
            return "[" + formatExpression(computed.getExpr(), depth + 1) + "]";
        }

        return "<key>";
    }

    private String quote(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }

    // ---------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------

    private String findCalleeName(Swc4jAstCallExpr call) {

        var callee = call.getCallee();

        if (callee instanceof Swc4jAstIdent identifier) {
            return identifier.getSym();
        }

        if (callee instanceof Swc4jAstMemberExpr member
                && member.getProp() instanceof Swc4jAstIdentName property) {
            return property.getSym();
        }

        List<Swc4jAstIdent> identifiers = callee.find(Swc4jAstIdent.class);

        if (identifiers.isEmpty()) {
            return null;
        }

        return identifiers.get(identifiers.size() - 1).getSym();
    }

    private boolean isComponentName(String name) {
        return name != null
                && !name.isBlank()
                && Character.isUpperCase(name.charAt(0));
    }

    private boolean isHookName(String name) {
        return name != null && name.matches("use[A-Z0-9].*");
    }

    // ---------------------------------------------------------
    // Internal records
    // ---------------------------------------------------------

    private record DetectedComponent(
            String name,
            String kind,
            ISwc4jAst node
    ) {
    }

    private record OwnedCall(
            Swc4jAstCallExpr call,
            String ownerName,
            String ownerKind
    ) {
    }

    private record HookDefinition(String name, ISwc4jAst node,
                                  List<String> parameters, boolean exported) {
    }
}

