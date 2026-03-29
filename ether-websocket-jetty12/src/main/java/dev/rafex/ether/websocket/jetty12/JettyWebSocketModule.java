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

/**
 * SPI contract for WebSocket modules that register endpoint routes.
 *
 * <p>Implementations define which WebSocket endpoints the application exposes
 * by calling {@link JettyWebSocketRouteRegistry#add} inside
 * {@link #registerRoutes}. The framework invokes this method once during
 * server bootstrap, passing the shared registry and a read-only context.</p>
 */
public interface JettyWebSocketModule {

    /**
     * Registers WebSocket routes on the given registry.
     *
     * <p>The default implementation is a no-op, so modules only need to
     * override this method when they expose actual endpoints.</p>
     *
     * @param routes  the route registry to register endpoints on
     * @param context the module context providing server configuration
     */
    default void registerRoutes(final JettyWebSocketRouteRegistry routes, final JettyWebSocketModuleContext context) {
    }
}
