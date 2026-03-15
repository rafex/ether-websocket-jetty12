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

public final class JettyWebSocketSession implements WebSocketSession {

	private final Session session;
	private final String path;
	private final Map<String, String> pathParams;
	private final Map<String, List<String>> queryParams;
	private final Map<String, List<String>> headers;
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	public JettyWebSocketSession(final Session session, final String path, final Map<String, String> pathParams,
			final Map<String, List<String>> queryParams, final Map<String, List<String>> headers) {
		this.session = Objects.requireNonNull(session, "session");
		this.path = Objects.requireNonNull(path, "path");
		this.pathParams = Map.copyOf(pathParams);
		this.queryParams = copyMultiMap(queryParams);
		this.headers = copyMultiMap(headers);
	}

	@Override
	public String id() {
		return Integer.toHexString(System.identityHashCode(session));
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public String subprotocol() {
		final var protocol = session.getUpgradeResponse() == null ? null : session.getUpgradeResponse().getAcceptedSubProtocol();
		return protocol == null ? "" : protocol;
	}

	@Override
	public boolean isOpen() {
		return session.isOpen();
	}

	@Override
	public String pathParam(final String name) {
		return pathParams.get(name);
	}

	@Override
	public String queryFirst(final String name) {
		final var values = queryParams.get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

	@Override
	public List<String> queryAll(final String name) {
		return queryParams.getOrDefault(name, List.of());
	}

	@Override
	public String headerFirst(final String name) {
		final var values = headers.get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

	@Override
	public Object attribute(final String name) {
		return attributes.get(name);
	}

	@Override
	public void attribute(final String name, final Object value) {
		if (value == null) {
			attributes.remove(name);
			return;
		}
		attributes.put(name, value);
	}

	@Override
	public Map<String, String> pathParams() {
		return pathParams;
	}

	@Override
	public Map<String, List<String>> queryParams() {
		return queryParams;
	}

	@Override
	public Map<String, List<String>> headers() {
		return headers;
	}

	@Override
	public CompletionStage<Void> sendText(final String text) {
		final var future = new CompletableFuture<Void>();
		session.sendText(text, callbackOf(future));
		return future;
	}

	@Override
	public CompletionStage<Void> sendBinary(final ByteBuffer data) {
		final var future = new CompletableFuture<Void>();
		session.sendBinary(data == null ? ByteBuffer.allocate(0) : data.slice(), callbackOf(future));
		return future;
	}

	@Override
	public CompletionStage<Void> close(final WebSocketCloseStatus status) {
		final var future = new CompletableFuture<Void>();
		final var closeStatus = status == null ? WebSocketCloseStatus.NORMAL : status;
		session.close(closeStatus.code(), closeStatus.reason(), callbackOf(future));
		return future;
	}

	private static Callback callbackOf(final CompletableFuture<Void> future) {
		return Callback.from(() -> future.complete(null), future::completeExceptionally);
	}

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
