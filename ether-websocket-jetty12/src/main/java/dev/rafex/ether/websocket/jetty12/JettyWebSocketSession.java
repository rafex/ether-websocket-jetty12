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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketSession;

/**
 * Jetty 12 implementation of {@link WebSocketSession}.
 *
 * <p>Wraps the native Jetty {@link Session} and exposes the framework's
 * transport-agnostic WebSocket session contract. Each instance carries the
 * request path, path parameters, query parameters, headers and a
 * thread-safe attribute bag.</p>
 */
public final class JettyWebSocketSession implements WebSocketSession {

    private final Session session;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> headers;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Creates a session wrapper.
     *
     * @param session     the native Jetty WebSocket session
     * @param path        the matched request path
     * @param pathParams  extracted path parameters (e.g. {@code "channel" -> "abc"})
     * @param queryParams query string parameters
     * @param headers     HTTP headers from the upgrade request
     * @throws NullPointerException if {@code session} or {@code path} is {@code null}
     */
    public JettyWebSocketSession(final Session session, final String path, final Map<String, String> pathParams,
            final Map<String, List<String>> queryParams, final Map<String, List<String>> headers) {
        this.session = Objects.requireNonNull(session, "session");
        this.path = Objects.requireNonNull(path, "path");
        this.pathParams = Map.copyOf(pathParams);
        this.queryParams = copyMultiMap(queryParams);
        this.headers = copyMultiMap(headers);
    }

    /**
     * Returns a unique session identifier derived from the native session's
     * identity hash code.
     *
     * @return a hex-encoded identity hash of the underlying session
     */
    @Override
    public String id() {
        return Integer.toHexString(System.identityHashCode(session));
    }

    /**
     * Returns the matched request path.
     *
     * @return the request path
     */
    @Override
    public String path() {
        return path;
    }

    /**
     * Returns the accepted subprotocol, or an empty string if none was negotiated.
     *
     * @return the negotiated subprotocol, or {@code ""}
     */
    @Override
    public String subprotocol() {
        final var protocol = session.getUpgradeResponse() == null ? null
                : session.getUpgradeResponse().getAcceptedSubProtocol();
        return protocol == null ? "" : protocol;
    }

    /**
     * Returns whether the underlying session is still open.
     *
     * @return {@code true} if the session is open
     */
    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    /**
     * Returns the value of a single path parameter by name.
     *
     * @param name the path parameter name
     * @return the value, or {@code null} if the parameter does not exist
     */
    @Override
    public String pathParam(final String name) {
        return pathParams.get(name);
    }

    /**
     * Returns the first value of a query parameter.
     *
     * @param name the query parameter name
     * @return the first value, or {@code null} if absent
     */
    @Override
    public String queryFirst(final String name) {
        final var values = queryParams.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    /**
     * Returns all values of a query parameter.
     *
     * @param name the query parameter name
     * @return an unmodifiable list of values (never {@code null})
     */
    @Override
    public List<String> queryAll(final String name) {
        return queryParams.getOrDefault(name, List.of());
    }

    /**
     * Returns the first value of an HTTP header.
     *
     * @param name the header name (case-sensitive)
     * @return the first value, or {@code null} if absent
     */
    @Override
    public String headerFirst(final String name) {
        final var values = headers.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    /**
     * Returns a session-scoped attribute by name.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not set
     */
    @Override
    public Object attribute(final String name) {
        return attributes.get(name);
    }

    /**
     * Sets or removes a session-scoped attribute.
     *
     * <p>Passing {@code null} as the value removes the attribute.</p>
     *
     * @param name  the attribute name
     * @param value the attribute value, or {@code null} to remove
     */
    @Override
    public void attribute(final String name, final Object value) {
        if (value == null) {
            attributes.remove(name);
            return;
        }
        attributes.put(name, value);
    }

    /**
     * Returns an unmodifiable view of all path parameters.
     *
     * @return the path parameter map
     */
    @Override
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /**
     * Returns an unmodifiable view of all query parameters.
     *
     * @return the query parameter map
     */
    @Override
    public Map<String, List<String>> queryParams() {
        return queryParams;
    }

    /**
     * Returns an unmodifiable view of all HTTP headers.
     *
     * @return the header map
     */
    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Sends a text message asynchronously.
     *
     * @param text the text payload
     * @return a completion stage that completes when the message is sent
     */
    @Override
    public CompletionStage<Void> sendText(final String text) {
        final var future = new CompletableFuture<Void>();
        session.sendText(text, callbackOf(future));
        return future;
    }

    /**
     * Sends a binary message asynchronously.
     *
     * @param data the binary payload (an empty buffer is sent if {@code null})
     * @return a completion stage that completes when the message is sent
     */
    @Override
    public CompletionStage<Void> sendBinary(final ByteBuffer data) {
        final var future = new CompletableFuture<Void>();
        session.sendBinary(data == null ? ByteBuffer.allocate(0) : data.slice(), callbackOf(future));
        return future;
    }

    /**
     * Closes the session with the given status code and reason.
     *
     * @param status the close status; defaults to {@link WebSocketCloseStatus#NORMAL} if {@code null}
     * @return a completion stage that completes when the close handshake finishes
     */
    @Override
    public CompletionStage<Void> close(final WebSocketCloseStatus status) {
        final var future = new CompletableFuture<Void>();
        final var closeStatus = status == null ? WebSocketCloseStatus.NORMAL : status;
        session.close(closeStatus.code(), closeStatus.reason(), callbackOf(future));
        return future;
    }

    /**
     * Adapts a {@link CompletableFuture} into a Jetty {@link Callback}.
     *
     * @param future the future to complete on success or failure
     * @return a Jetty callback
     */
    private static Callback callbackOf(final CompletableFuture<Void> future) {
        return Callback.from(() -> future.complete(null), future::completeExceptionally);
    }

    /**
     * Returns an unmodifiable deep copy of the given multi-valued map.
     *
     * @param input the source map
     * @return an unmodifiable copy
     */
    private static Map<String, List<String>> copyMultiMap(final Map<String, List<String>> input) {
        final var out = new LinkedHashMap<String, List<String>>();
        if (input != null) {
            for (final var entry : input.entrySet()) {
                out.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(out);
    }
}
