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
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketEndpoint;

/**
 * Adapta una instancia de {@link WebSocketEndpoint} al Listener de Jetty 12.
 *
 * <p>Esta clase traduce los eventos del ciclo de vida de Jetty a las llamadas
 * definidas en la interfaz {@link WebSocketEndpoint}.</p>
 */
final class JettyWebSocketEndpointAdapter implements Session.Listener.AutoDemanding {

    private final WebSocketEndpoint endpoint;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> headers;
    private volatile JettyWebSocketSession session;

    /**
     * Crea un adaptador para el endpoint dado.
     *
     * @param endpoint   el endpoint personalizado que manejará los eventos
     * @param path       la ruta asociada a la conexión
     * @param pathParams parámetros extraídos de la ruta
     * @param queryParams parámetros de la consulta HTTP
     * @param headers    cabeceras HTTP
     */
    JettyWebSocketEndpointAdapter(final WebSocketEndpoint endpoint, final String path,
            final Map<String, String> pathParams, final Map<String, List<String>> queryParams,
            final Map<String, List<String>> headers) {
        this.endpoint = endpoint;
        this.path = path;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.headers = headers;
    }

    @Override
    public void onWebSocketOpen(final Session session) {
        this.session = new JettyWebSocketSession(session, path, pathParams, queryParams, headers);
        try {
            endpoint.onOpen(this.session);
        } catch (final Exception e) {
            handleFailure(e);
        }
    }

    @Override
    public void onWebSocketText(final String message) {
        try {
            endpoint.onText(session, message);
        } catch (final Exception e) {
            handleFailure(e);
        }
    }

    @Override
    public void onWebSocketBinary(final ByteBuffer payload, final Callback callback) {
        try {
            endpoint.onBinary(session, payload == null ? ByteBuffer.allocate(0) : payload.slice());
            callback.succeed();
        } catch (final Exception e) {
            callback.fail(e);
            handleFailure(e);
        }
    }

    @Override
    public void onWebSocketPartialText(final String payload, final boolean fin) {
        if (fin) {
            onWebSocketText(payload);
        }
    }

    @Override
    public void onWebSocketPartialBinary(final ByteBuffer payload, final boolean fin, final Callback callback) {
        if (fin) {
            onWebSocketBinary(payload, callback);
            return;
        }
        callback.succeed();
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason, final Callback callback) {
        if (session == null) {
            callback.succeed();
            return;
        }
        try {
            endpoint.onClose(session, WebSocketCloseStatus.of(statusCode, reason));
            callback.succeed();
        } catch (final Exception e) {
            endpoint.onError(session, e);
            callback.fail(e);
        }
    }

    private void handleFailure(final Exception error) {
        endpoint.onError(session, error);
        if (session != null && session.isOpen()) {
            session.close(WebSocketCloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void onWebSocketError(final Throwable cause) {
        if (session == null) {
            return;
        }
        endpoint.onError(session, cause);
    }
}
