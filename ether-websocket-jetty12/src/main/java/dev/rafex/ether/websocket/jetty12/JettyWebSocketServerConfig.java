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

import java.util.Map;

/**
 * Immutable configuration for the Jetty WebSocket server.
 *
 * <p>Encapsulates port, thread pool sizing and idle timeout. Use
 * {@link #fromEnv()} to populate the values from environment variables.</p>
 *
 * @param port           the HTTP port the server binds to
 * @param minThreads     minimum threads in the Jetty thread pool
 * @param maxThreads     maximum threads in the Jetty thread pool
 * @param idleTimeoutMs  idle timeout in milliseconds for the thread pool
 * @param threadPoolName the name prefix for Jetty thread pool threads
 */
public record JettyWebSocketServerConfig(int port, int minThreads, int maxThreads, int idleTimeoutMs,
        String threadPoolName) {

    /**
     * Creates a configuration from the current process environment.
     *
     * <p>Delegates to {@link #fromEnv(Map)} using {@link System#getenv()}.</p>
     *
     * @return a config populated from environment variables
     * @see #fromEnv(Map)
     */
    public static JettyWebSocketServerConfig fromEnv() {
        return fromEnv(System.getenv());
    }

    /**
     * Creates a configuration from the given environment map.
     *
     * <p>Recognised keys and their defaults:</p>
     * <ul>
     *   <li>{@code HTTP_PORT} — default {@code 8080}</li>
     *   <li>{@code HTTP_MIN_THREADS} — default {@code 4}</li>
     *   <li>{@code HTTP_MAX_THREADS} — default {@code 32}</li>
     *   <li>{@code HTTP_IDLE_TIMEOUT_MS} — default {@code 30000}</li>
     *   <li>{@code HTTP_POOL_NAME} — default {@code "ether-websocket"}</li>
     * </ul>
     *
     * @param env the environment map to read values from
     * @return a config populated from the map
     */
    public static JettyWebSocketServerConfig fromEnv(final Map<String, String> env) {
        return new JettyWebSocketServerConfig(parseInt(env.get("HTTP_PORT"), 8080),
                parseInt(env.get("HTTP_MIN_THREADS"), 4), parseInt(env.get("HTTP_MAX_THREADS"), 32),
                parseInt(env.get("HTTP_IDLE_TIMEOUT_MS"), 30_000),
                env.getOrDefault("HTTP_POOL_NAME", "ether-websocket"));
    }

    /**
     * Parses an integer value, falling back to the default on {@code null},
     * blank or non-numeric input.
     *
     * @param value    the raw string to parse
     * @param fallback the value to use when parsing fails
     * @return the parsed integer, or the fallback
     */
    private static int parseInt(final String value, final int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }
}
