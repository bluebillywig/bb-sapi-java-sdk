package com.bluebillywig.sapi;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

/** Per-request options for {@link Sdk#sendRequest(String, String, RequestOptions)}. */
public final class RequestOptions {
    private Map<String, String> query;
    private Object json;
    private HttpRequest.BodyPublisher body;
    private Map<String, String> headers;
    private Boolean skipAuth;
    private Duration timeout;

    /** @return the query parameters, or {@code null} */
    public Map<String, String> getQuery() {
        return query;
    }

    /**
     * Query parameters to set on the URL (replacing same-named existing ones), in iteration order.
     *
     * @param query the parameters, or {@code null}
     * @return this
     */
    public RequestOptions setQuery(Map<String, String> query) {
        this.query = query;
        return this;
    }

    /** @return the JSON body object, or {@code null} */
    public Object getJson() {
        return json;
    }

    /**
     * An object serialized as the JSON body with {@link com.bluebillywig.sapi.util.SapiJson}
     * (sets {@code Content-Type: application/json}).
     *
     * @param json the object, or {@code null}
     * @return this
     */
    public RequestOptions setJson(Object json) {
        this.json = json;
        return this;
    }

    /** @return the raw body, or {@code null} */
    public HttpRequest.BodyPublisher getBody() {
        return body;
    }

    /**
     * A raw body, used when {@link #setJson(Object)} is not set. Use a sized publisher such as
     * {@code BodyPublishers.ofByteArray}; an {@code ofInputStream} body goes out chunked.
     *
     * @param body the body, or {@code null}
     * @return this
     */
    public RequestOptions setBody(HttpRequest.BodyPublisher body) {
        this.body = body;
        return this;
    }

    /** @return the extra request headers, or {@code null} */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Extra request headers. These override authentication headers of the same name
     * (case-insensitively). {@code java.net.http} forbids setting {@code Content-Length},
     * {@code Host}, {@code Connection}, {@code Expect} and {@code Upgrade}.
     *
     * @param headers the headers, or {@code null}
     * @return this
     */
    public RequestOptions setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /** @return the skip-auth override, or {@code null} for the same-origin default */
    public Boolean getSkipAuth() {
        return skipAuth;
    }

    /**
     * Force-skip the SAPI auth headers for this request. By default auth is attached only to
     * same-origin (SAPI) requests; cross-origin URLs (e.g. S3 presigned upload/progress URLs)
     * are never sent the rpctoken. Set this to also suppress auth on a same-origin request.
     *
     * @param skipAuth whether to skip auth, or {@code null} for the default
     * @return this
     */
    public RequestOptions setSkipAuth(Boolean skipAuth) {
        this.skipAuth = skipAuth;
        return this;
    }

    /** @return the per-request timeout override, or {@code null} */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Per-request timeout, overriding {@link SdkOptions#setTimeout(Duration)}. Pass
     * {@link Duration#ZERO} to disable the timeout for this request (used by uploads).
     *
     * @param timeout the timeout, or {@code null} for the SDK default
     * @return this
     */
    public RequestOptions setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }
}
