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

public record JettyWebSocketServerConfig(int port, int minThreads, int maxThreads, int idleTimeoutMs,
		String threadPoolName) {

	public static JettyWebSocketServerConfig fromEnv() {
		return fromEnv(System.getenv());
	}

	public static JettyWebSocketServerConfig fromEnv(final Map<String, String> env) {
		return new JettyWebSocketServerConfig(parseInt(env.get("HTTP_PORT"), 8080), parseInt(env.get("HTTP_MIN_THREADS"), 4),
				parseInt(env.get("HTTP_MAX_THREADS"), 32), parseInt(env.get("HTTP_IDLE_TIMEOUT_MS"), 30_000),
				env.getOrDefault("HTTP_POOL_NAME", "ether-websocket"));
	}

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
