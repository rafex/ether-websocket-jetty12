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

public final class JettyWebSocketServerFactory {

	private JettyWebSocketServerFactory() {
	}

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

	public static JettyWebSocketServerRunner create(final JettyWebSocketServerConfig config,
			final List<JettyWebSocketModule> modules) {
		final var routeRegistry = new JettyWebSocketRouteRegistry();
		final var context = new JettyWebSocketModuleContext(config);
		for (final var module : modules == null ? List.<JettyWebSocketModule>of() : modules) {
			module.registerRoutes(routeRegistry, context);
		}
		return create(config, routeRegistry);
	}

	private static Handler buildUpgradeHandler(final Server server, final List<WebSocketRoute> routes) {
		return WebSocketUpgradeHandler.from(server, container -> {
			for (final var route : routes == null ? List.<WebSocketRoute>of() : new ArrayList<>(routes)) {
				container.addMapping(PathSpec.from(route.pattern()),
						(request, response, callback) -> createEndpoint(route, request, response));
			}
		});
	}

	private static Object createEndpoint(final WebSocketRoute route, final ServerUpgradeRequest request,
			final ServerUpgradeResponse response) {
		final var path = request.getHttpURI().getPath();
		final var pathParams = WebSocketPatterns.match(route.pattern(), path).orElse(Map.of());
		final var headers = headersOf(request);
		final var queryParams = queryOf(request);
		negotiateSubprotocol(route, request, response);
		return new JettyWebSocketEndpointAdapter(route.endpoint(), path, pathParams, queryParams, headers);
	}

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

	private static Map<String, List<String>> headersOf(final ServerUpgradeRequest request) {
		final var out = new LinkedHashMap<String, List<String>>();
		for (final var field : request.getHeaders()) {
			out.computeIfAbsent(field.getName(), ignored -> new ArrayList<>()).add(field.getValue());
		}
		return copyMultiMap(out);
	}

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

	private static Map<String, List<String>> copyMultiMap(final Map<String, List<String>> input) {
		final var out = new LinkedHashMap<String, List<String>>();
		for (final var entry : input.entrySet()) {
			out.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
		}
		return Map.copyOf(out);
	}
}
