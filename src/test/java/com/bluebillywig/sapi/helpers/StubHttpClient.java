package com.bluebillywig.sapi.helpers;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * A scripted {@link HttpClient} for the cases a real socket cannot reproduce exactly: a failure
 * with a known cause instance, a request that never completes, and "must not be called". The
 * counterpart of the .NET tests' {@code ThrowingHandler}.
 */
public final class StubHttpClient extends HttpClient {
    private final Throwable failure;
    private final AtomicInteger sends = new AtomicInteger();
    private volatile CompletableFuture<?> lastFuture;

    private StubHttpClient(Throwable failure) {
        this.failure = failure;
    }

    /** A client whose every request fails with {@code cause}. */
    public static StubHttpClient failing(Throwable cause) {
        return new StubHttpClient(cause);
    }

    /** A client whose requests never complete. */
    public static StubHttpClient hanging() {
        return new StubHttpClient(null);
    }

    public int sends() {
        return sends.get();
    }

    /** @return the future handed out for the last request */
    public CompletableFuture<?> lastFuture() {
        return lastFuture;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        sends.incrementAndGet();
        CompletableFuture<HttpResponse<T>> future = new CompletableFuture<>();
        lastFuture = future;
        if (failure != null) {
            future.completeExceptionally(failure);
        }
        return future;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("the SDK uses sendAsync");
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }

    @Override
    public Redirect followRedirects() {
        return Redirect.NEVER;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.empty();
    }

    @Override
    public SSLContext sslContext() {
        try {
            return SSLContext.getDefault();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SSLParameters sslParameters() {
        return new SSLParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }

    @Override
    public Version version() {
        return Version.HTTP_1_1;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }
}
