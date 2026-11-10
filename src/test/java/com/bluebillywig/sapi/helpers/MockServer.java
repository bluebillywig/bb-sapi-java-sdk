package com.bluebillywig.sapi.helpers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A real HTTP server on 127.0.0.1 (port chosen by the OS) that returns pre-configured responses in
 * FIFO order and records every request exactly as it arrived on the wire. The Java counterpart of
 * the .NET {@code MockHttpHandler}, but a socket-level one, so Content-Length, transfer encoding,
 * query encoding and headers are the real thing.
 */
public final class MockServer implements AutoCloseable {
    /** A response to hand out. */
    public record MockResponse(int status, Map<String, String> headers, String body, long delayMs) {
        public static MockResponse ok() {
            return new MockResponse(200, Map.of(), null, 0);
        }

        public static MockResponse ok(String body) {
            return new MockResponse(200, Map.of(), body, 0);
        }

        public static MockResponse status(int status) {
            return new MockResponse(status, Map.of(), null, 0);
        }

        public static MockResponse withHeader(String name, String value) {
            return new MockResponse(200, Map.of(name, value), null, 0);
        }

        public MockResponse delayed(long millis) {
            return new MockResponse(status, headers, body, millis);
        }
    }

    /** One recorded request. */
    public static final class RecordedCall {
        private final String url;
        private final String method;
        private final Map<String, String> headers;
        private final byte[] body;

        RecordedCall(String url, String method, Map<String, String> headers, byte[] body) {
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.body = body;
        }

        /** @return the full URL as the client sent it (raw path and query, so encoding is visible) */
        public String url() {
            return url;
        }

        public String method() {
            return method;
        }

        public byte[] body() {
            return body;
        }

        public String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /** @return the request header (case-insensitive), or null */
        public String header(String name) {
            return headers.get(name);
        }
    }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mock-server");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentLinkedQueue<MockResponse> queue = new ConcurrentLinkedQueue<>();
    private final List<RecordedCall> calls = new ArrayList<>();
    private final String baseUri;

    public MockServer(MockResponse... responses) {
        queue.addAll(List.of(responses));
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
        baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** @return the origin of this server, e.g. {@code http://127.0.0.1:4242} */
    public String baseUri() {
        return baseUri;
    }

    /** @return the absolute URL of a path (and query) on this server */
    public String url(String pathAndQuery) {
        return baseUri + pathAndQuery;
    }

    /** @return a snapshot of the recorded calls, in arrival order */
    public List<RecordedCall> calls() {
        synchronized (calls) {
            return new ArrayList<>(calls);
        }
    }

    public RecordedCall call(int index) {
        return calls().get(index);
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] body;
        try (InputStream in = exchange.getRequestBody()) {
            body = in.readAllBytes();
        }

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> h : exchange.getRequestHeaders().entrySet()) {
            headers.put(h.getKey(), String.join(", ", h.getValue()));
        }
        String rawQuery = exchange.getRequestURI().getRawQuery();
        String url = baseUri + exchange.getRequestURI().getRawPath() + (rawQuery == null ? "" : "?" + rawQuery);

        MockResponse mock;
        int callNumber;
        synchronized (calls) {
            calls.add(new RecordedCall(url, exchange.getRequestMethod(), headers, body));
            callNumber = calls.size();
            mock = queue.poll();
        }

        try {
            if (mock == null) {
                byte[] msg = ("No more mock responses available (call #" + callNumber + ")").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(599, msg.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(msg);
                }
                return;
            }
            if (mock.delayMs() > 0) {
                Thread.sleep(mock.delayMs());
            }
            Map<String, String> responseHeaders = new LinkedHashMap<>(mock.headers());
            responseHeaders.forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            byte[] payload = mock.body() == null ? new byte[0] : mock.body().getBytes(StandardCharsets.UTF_8);
            boolean noBody = payload.length == 0 || exchange.getRequestMethod().equals("HEAD");
            if (noBody) {
                exchange.sendResponseHeaders(mock.status(), -1);
            } else {
                exchange.sendResponseHeaders(mock.status(), payload.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload);
                }
            }
        } catch (IOException e) {
            // The client went away (timeout / cancel); nothing to answer.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
