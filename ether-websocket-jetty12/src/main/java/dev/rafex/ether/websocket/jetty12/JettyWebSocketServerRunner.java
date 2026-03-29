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

import org.eclipse.jetty.server.Server;

/**
 * Thin wrapper around a Jetty {@link Server} that exposes lifecycle methods.
 *
 * <p>Created exclusively by {@link JettyWebSocketServerFactory}. Callers use
 * {@link #start()}, {@link #join()} and {@link #stop()} to control the server
 * lifecycle.</p>
 */
public final class JettyWebSocketServerRunner {

    private final Server server;

    /**
     * Package-private constructor. Use {@link JettyWebSocketServerFactory} instead.
     *
     * @param server the underlying Jetty server
     */
    JettyWebSocketServerRunner(final Server server) {
        this.server = server;
    }

    /**
     * Starts the underlying Jetty server.
     *
     * @throws Exception if the server fails to start
     */
    public void start() throws Exception {
        server.start();
    }

    /**
     * Blocks the calling thread until the server is stopped.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void join() throws InterruptedException {
        server.join();
    }

    /**
     * Stops the underlying Jetty server.
     *
     * @throws Exception if the server fails to stop cleanly
     */
    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Returns the underlying Jetty {@link Server} for advanced use cases.
     *
     * @return the Jetty server instance
     */
    public Server server() {
        return server;
    }
}
