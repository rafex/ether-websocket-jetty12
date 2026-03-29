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

import java.util.ArrayList;
import java.util.List;

import dev.rafex.ether.websocket.core.WebSocketEndpoint;
import dev.rafex.ether.websocket.core.WebSocketRoute;

/**
 * Mutable registry that collects WebSocket routes during module registration.
 *
 * <p>Modules call {@link #add} to bind a URL path pattern to a
 * {@link WebSocketEndpoint}. After all modules have been processed, the
 * factory reads the accumulated routes via {@link #routes}.</p>
 */
public final class JettyWebSocketRouteRegistry {

    private final List<WebSocketRoute> routes = new ArrayList<>();

    /**
     * Registers a WebSocket endpoint under the given URL path pattern.
     *
     * @param pattern  the URL path pattern (e.g. {@code "/ws/{channel}"})
     * @param endpoint the WebSocket endpoint to invoke when the pattern matches
     */
    public void add(final String pattern, final WebSocketEndpoint endpoint) {
        routes.add(WebSocketRoute.of(pattern, endpoint));
    }

    /**
     * Returns an unmodifiable snapshot of all registered routes.
     *
     * @return a copy of the current route list
     */
    public List<WebSocketRoute> routes() {
        return List.copyOf(routes);
    }
}
