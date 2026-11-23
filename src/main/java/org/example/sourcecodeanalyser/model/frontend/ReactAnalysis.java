
package org.example.sourcecodeanalyser.model.frontend;

import java.util.List;
import java.util.Map;

public record ReactAnalysis(
        List<ComponentInfo> components,
        List<HookInfo> hooks,
        List<ApiInteractionInfo> apiInteractions,
        List<AxiosInstanceInfo> axiosInstances,
        List<CustomHookInfo> customHooks,
        List<RouteInfo> routes,
        List<NavigationInfo> navigations,
        List<RouterHookInfo> routerHooks,
        List<FunctionInfo> functions
) {

    // Backward-compatible constructors
    public ReactAnalysis(
            List<ComponentInfo> components,
            List<HookInfo> hooks
    ) {
        this(components, hooks, List.of());
    }

    public ReactAnalysis(
            List<ComponentInfo> components,
            List<HookInfo> hooks,
            List<ApiInteractionInfo> apiInteractions
    ) {
        this(
                components,
                hooks,
                apiInteractions,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public record ComponentInfo(
            String name,
            String kind,
            boolean exported,
            boolean defaultExport,
            List<String> props,
            List<StateInfo> state
    ) {}

    public record StateInfo(
            String name,
            String setter,
            String hook,
            String initialValue
    ) {}

    public record HookInfo(
            String name,
            String componentName,
            List<String> arguments,
            boolean custom,
            String ownerName,
            String ownerKind
    ) {
        public HookInfo(
                String name,
                String componentName,
                List<String> arguments,
                boolean custom
        ) {
            this(name, componentName, arguments, custom, componentName, "COMPONENT");
        }
    }

    public record ApiInteractionInfo(
            String componentName,
            String client,
            String method,
            String endpoint,
            List<String> arguments,
            String ownerName,
            String ownerKind,
            Map<String, String> headers,
            Map<String, String> queryParams,
            String body
    ) {
        public ApiInteractionInfo(
                String componentName,
                String client,
                String method,
                String endpoint,
                List<String> arguments
        ) {
            this(
                    componentName,
                    client,
                    method,
                    endpoint,
                    arguments,
                    componentName,
                    "COMPONENT",
                    Map.of(),
                    Map.of(),
                    null
            );
        }
    }

    public record AxiosInstanceInfo(
            String name,
            String source,
            String baseUrl,
            Map<String, String> headers,
            String timeout
    ) {}

    public record CustomHookInfo(
            String name,
            List<String> parameters,
            boolean exported,
            List<String> hooksUsed,
            List<String> componentsUsing
    ) {}

    public record RouteInfo(
            String path,
            String componentName,
            String element,
            String parentPath,
            String routeType
    ) {}

    public record NavigationInfo(
            String componentName,
            String ownerName,
            String ownerKind,
            String method,
            String destination,
            List<String> arguments
    ) {}

    public record RouterHookInfo(
            String name,
            String componentName,
            String ownerName,
            String ownerKind,
            List<String> arguments
    ) {}

    public record FunctionInfo(
            String name,
            String parentName,
            String kind,
            List<String> parameters,
            String componentName
    ) {}
}
