package com.bluebillywig.sapi;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/** Optional SDK configuration. */
public final class SdkOptions {
    private String baseUri;
    private HttpClient httpClient;
    private Duration timeout = Duration.ofSeconds(30);

    /** @return the base URI override, or {@code null} for {@code https://{publication}.bbvms.com} */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Overrides the base URI (e.g. for testing). Defaults to {@code https://{publication}.bbvms.com}.
     *
     * @param baseUri the base URI, or {@code null} for the default
     * @return this
     */
    public SdkOptions setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    /** @return the injected client, or {@code null} when the SDK creates its own */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Injects a {@link HttpClient} (for testing, or to share a pooled client with your own
     * configuration such as proxy, TLS or executor). When omitted the SDK creates one. The SDK
     * manages request timeouts itself (see {@link #setTimeout(Duration)}), by waiting on
     * {@link HttpClient#sendAsync}; an injected client must therefore implement
     * {@code sendAsync} (a plain {@code HttpClient} always does). Redirects are not followed by
     * the SDK's own client, so the {@code rpctoken} can never be replayed to another origin; set
     * {@link HttpClient.Redirect} on your own client only if you accept that.
     *
     * @param httpClient the client, or {@code null}
     * @return this
     */
    public SdkOptions setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /** @return the default per-request timeout */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Default per-request timeout. A request that produces no complete response within this window
     * is aborted and surfaces as a {@link com.bluebillywig.sapi.exceptions.SapiConnectionException}.
     * Pass {@link Duration#ZERO} to disable the default timeout. Defaults to 30 seconds.
     * Override per request via {@link RequestOptions#setTimeout(Duration)} (streaming uploads pass
     * zero, since their duration scales with file size).
     *
     * @param timeout the timeout
     * @return this
     */
    public SdkOptions setTimeout(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        return this;
    }
}
