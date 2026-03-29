package dev.rafex.ether.websocket.jetty12;

/*-
 * #%L
 * ether-websocket-jetty12
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import dev.rafex.ether.websocket.core.WebSocketPatterns;
import dev.rafex.ether.websocket.core.WebSocketRoute;

/**
 * Factory that wires a Jetty {@link Server} with WebSocket routing.
 *
 * <p>This utility class provides static factory methods to create a
 * {@link JettyWebSocketServerRunner} from a configuration and either a
 * pre-built route registry or a list of {@link JettyWebSocketModule} instances.</p>
 */
public final class JettyWebSocketServerFactory {

    private JettyWebSocketServerFactory() {
    }

    /**
     * Creates a WebSocket server from the given configuration and route registry.
     *
     * <p>Configures a {@link QueuedThreadPool}, a {@link ServerConnector}, and a
     * {@link WebSocketUpgradeHandler} that maps incoming upgrade requests to the
     * registered endpoints.</p>
     *
     * @param config        the server configuration (port, threads, timeout)
     * @param routeRegistry the registry containing the WebSocket routes
     * @return a runner that owns the configured {@link Server}
     * @throws NullPointerException if {@code config} or {@code routeRegistry} is {@code null}
     */
    public static JettyWebSocketServerRunner create(final JettyWebSocketServerConfig config,
            final JettyWebSocketRouteRegistry routeRegistry) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(routeRegistry, "routeRegistry");

        final var pool = new QueuedThreadPool();
        pool.setMaxThreads(config.maxThreads());
        pool.setMinThreads(config.minThreads());
        pool.setIdleTimeout(config.idleTimeoutMs());
        pool.setName(config.threadPoolName());

        final var server = new Server(pool);
        final var connector = new ServerConnector(server);
        connector.setPort(config.port());
        server.addConnector(connector);

        final var context = new ContextHandler("/");
        context.setHandler(buildUpgradeHandler(server, routeRegistry.routes()));
        server.setHandler(context);
        return new JettyWebSocketServerRunner(server);
    }

    /**
     * Creates a WebSocket server by invoking all given modules to populate the
     * route registry, then delegates to {@link #create(JettyWebSocketServerConfig,
     * JettyWebSocketRouteRegistry)}.
     *
     * @param config  the server configuration
     * @param modules the list of modules whose {@code registerRoutes} will be called
     * @return a runner that owns the configured {@link Server}
     */
    public static JettyWebSocketServerRunner create(final JettyWebSocketServerConfig config,
            final List<JettyWebSocketModule> modules) {
        final var routeRegistry = new JettyWebSocketRouteRegistry();
        final var context = new JettyWebSocketModuleContext(config);
        for (final var module : modules == null ? List.<JettyWebSocketModule>of() : modules) {
            module.registerRoutes(routeRegistry, context);
        }
        return create(config, routeRegistry);
    }

    /**
     * Builds a {@link WebSocketUpgradeHandler} that maps each route's path spec
     * to its endpoint adapter.
     *
     * @param server the Jetty server instance
     * @param routes the WebSocket routes to register
     * @return a configured upgrade handler
     */
    private static Handler buildUpgradeHandler(final Server server, final List<WebSocketRoute> routes) {
        return WebSocketUpgradeHandler.from(server, container -> {
            for (final var route : routes == null ? List.<WebSocketRoute>of() : new ArrayList<>(routes)) {
                container.addMapping(PathSpec.from(route.pattern()),
                        (request, response, callback) -> createEndpoint(route, request, response));
            }
        });
    }

    /**
     * Creates the endpoint adapter for a WebSocket upgrade request by extracting
     * path params, headers, query params and negotiating subprotocols.
     *
     * @param route    the matched route
     * @param request  the upgrade request
     * @param response the upgrade response
     * @return the endpoint adapter instance
     */
    private static Object createEndpoint(final WebSocketRoute route, final ServerUpgradeRequest request,
            final ServerUpgradeResponse response) {
        final var path = request.getHttpURI().getPath();
        final var pathParams = WebSocketPatterns.match(route.pattern(), path).orElse(Map.of());
        final var headers = headersOf(request);
        final var queryParams = queryOf(request);
        negotiateSubprotocol(route, request, response);
        return new JettyWebSocketEndpointAdapter(route.endpoint(), path, pathParams, queryParams, headers);
    }

    /**
     * Negotiates a subprotocol if the endpoint supports any. Picks the first
     * requested subprotocol that the endpoint declares as supported.
     *
     * @param route    the matched route
     * @param request  the upgrade request
     * @param response the upgrade response to set the accepted subprotocol on
     */
    private static void negotiateSubprotocol(final WebSocketRoute route, final ServerUpgradeRequest request,
            final ServerUpgradeResponse response) {
        final var supported = route.endpoint().subprotocols();
        if (supported == null || supported.isEmpty()) {
            return;
        }
        for (final var requested : request.getSubProtocols()) {
            if (supported.contains(requested)) {
                response.setAcceptedSubProtocol(requested);
                return;
            }
        }
    }

    /**
     * Extracts HTTP headers from the upgrade request into an unmodifiable map.
     *
     * @param request the upgrade request
     * @return an unmodifiable map of header name to list of values
     */
    private static Map<String, List<String>> headersOf(final ServerUpgradeRequest request) {
        final var out = new LinkedHashMap<String, List<String>>();
        for (final var field : request.getHeaders()) {
            out.computeIfAbsent(field.getName(), ignored -> new ArrayList<>()).add(field.getValue());
        }
        return copyMultiMap(out);
    }

    /**
     * Decodes the query string from the upgrade request into an unmodifiable map.
     *
     * @param request the upgrade request
     * @return an unmodifiable map of query parameter name to list of values
     */
    private static Map<String, List<String>> queryOf(final ServerUpgradeRequest request) {
        final MultiMap<String> params = new MultiMap<>();
        final var rawQuery = request.getHttpURI().getQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
        }

        final var out = new LinkedHashMap<String, List<String>>();
        for (final var key : params.keySet()) {
            final var values = params.getValues(key);
            out.put(key, values == null ? List.of() : List.copyOf(values));
        }
        return Map.copyOf(out);
    }

    /**
     * Returns an unmodifiable deep copy of the given multi-valued map.
     *
     * @param input the source map
     * @return an unmodifiable copy
     */
    private static Map<String, List<String>> copyMultiMap(final Map<String, List<String>> input) {
        final var out = new LinkedHashMap<String, List<String>>();
        for (final var entry : input.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
        }
        return Map.copyOf(out);
    }
}
