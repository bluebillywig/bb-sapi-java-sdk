package com.bluebillywig.sapi;

import com.bluebillywig.sapi.exceptions.SapiClientErrorException;
import com.bluebillywig.sapi.exceptions.SapiRequestException;
import com.bluebillywig.sapi.exceptions.SapiServerErrorException;
import com.bluebillywig.sapi.util.HttpStatusCodeCategory;
import com.bluebillywig.sapi.util.QueryParams;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Represents a SAPI HTTP response with convenience accessors. */
public final class SapiResponse {
    private final String url;
    private final String method;
    private final int statusCode;
    private final String statusText;
    private final Map<String, String> headers;
    private final String body;

    /**
     * Creates a response.
     *
     * @param url the final resolved URL of the request (including query params)
     * @param method the HTTP method used for the request
     * @param statusCode the HTTP status code
     * @param statusText the HTTP status text (reason phrase)
     * @param headers the response headers
     * @param body the raw response body
     */
    public SapiResponse(String url, String method, int statusCode, String statusText, Map<String, String> headers, String body) {
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.statusText = statusText == null ? "" : statusText;
        Map<String, String> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(headers);
        this.headers = Collections.unmodifiableMap(copy);
        this.body = body == null ? "" : body;
    }

    /** @return the final resolved URL of the request (including query params) */
    public String getUrl() {
        return url;
    }

    /** @return the HTTP method used for the request */
    public String getMethod() {
        return method;
    }

    /** @return the HTTP status code */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * The HTTP status text (reason phrase). For responses received from the network this is the
     * standard phrase for the status code, because {@code java.net.http} does not expose the
     * phrase that came over the wire.
     *
     * @return the status text
     */
    public String getStatusText() {
        return statusText;
    }

    /** @return the response headers (case-insensitive keys) */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /** @return the raw response body as a string */
    public String getBody() {
        return body;
    }

    /** @return {@code true} if the response has a 2xx status code */
    public boolean isOk() {
        return getStatusCategory() == HttpStatusCodeCategory.SUCCESSFUL;
    }

    /**
     * @return the HTTP status code category (informational, successful, etc.)
     * @throws IllegalArgumentException when the status code is outside 100-599
     */
    public HttpStatusCodeCategory getStatusCategory() {
        return HttpStatusCodeCategory.of(statusCode);
    }

    /**
     * Throws an appropriate exception if the response is not 2xx.
     *
     * @throws SapiClientErrorException for 4xx responses
     * @throws SapiServerErrorException for 5xx responses
     * @throws SapiRequestException for other non-2xx responses
     */
    public void assertOk() {
        if (isOk()) {
            return;
        }

        String responseBody = body.isEmpty() ? null : body;
        switch (getStatusCategory()) {
            case CLIENT_ERROR:
                throw new SapiClientErrorException(statusText, statusCode, responseBody);
            case SERVER_ERROR:
                throw new SapiServerErrorException(statusText, statusCode, responseBody);
            default:
                throw new SapiRequestException(statusText, statusCode, responseBody);
        }
    }

    /**
     * Parses the response body as a JSON tree.
     *
     * @return the parsed tree, or {@code null} if the body is empty
     * @throws UncheckedIOException if the body is not valid JSON
     */
    public JsonNode json() {
        return body.isEmpty() ? null : SapiJson.readTree(body);
    }

    /**
     * Deserializes the response body as {@code type} with {@link SapiJson}.
     *
     * @param type the target type
     * @param <T> the target type
     * @return the deserialized value, or {@code null} if the body is empty
     * @throws UncheckedIOException if the body is not valid JSON for the type
     */
    public <T> T json(Class<T> type) {
        return body.isEmpty() ? null : SapiJson.read(body, type);
    }

    /**
     * Deserializes the response body as a generic type with {@link SapiJson}.
     *
     * @param type the target type
     * @param <T> the target type
     * @return the deserialized value, or {@code null} if the body is empty
     * @throws UncheckedIOException if the body is not valid JSON for the type
     */
    public <T> T json(TypeReference<T> type) {
        return body.isEmpty() ? null : SapiJson.read(body, type);
    }

    /**
     * Returns a response header value by name (case-insensitive).
     *
     * @param name the header name
     * @return the value, or {@code null}
     */
    public String header(String name) {
        return headers.get(name);
    }

    /**
     * Returns a query parameter from the request URL.
     *
     * @param name the parameter name
     * @return the decoded value, or {@code null} if not present
     */
    public String queryParam(String name) {
        return QueryParams.get(url, name);
    }

    /**
     * Returns {@code true} if all responses have 2xx status codes.
     *
     * @param responses the responses
     * @return whether all are 2xx
     */
    public static boolean allOk(Iterable<SapiResponse> responses) {
        for (SapiResponse response : responses) {
            if (!response.isOk()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Asserts all responses are 2xx. Throws on the first non-2xx response.
     *
     * @param responses the responses
     */
    public static void assertAllOk(Iterable<SapiResponse> responses) {
        for (SapiResponse response : responses) {
            response.assertOk();
        }
    }

    /**
     * Returns only the failed (non-2xx) responses.
     *
     * @param responses the responses
     * @return the failed responses, in order
     */
    public static List<SapiResponse> failedResponses(Iterable<SapiResponse> responses) {
        List<SapiResponse> failed = new ArrayList<>();
        for (SapiResponse response : responses) {
            if (!response.isOk()) {
                failed.add(response);
            }
        }
        return failed;
    }
}
