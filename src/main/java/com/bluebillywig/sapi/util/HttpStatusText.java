package com.bluebillywig.sapi.util;

import java.util.Map;

/**
 * Standard HTTP reason phrases. {@code java.net.http} does not expose the reason phrase that came
 * over the wire, so {@link com.bluebillywig.sapi.SapiResponse#getStatusText()} is derived from the
 * status code with this table.
 */
public final class HttpStatusText {
    private static final Map<Integer, String> PHRASES = Map.ofEntries(
            Map.entry(100, "Continue"),
            Map.entry(101, "Switching Protocols"),
            Map.entry(200, "OK"),
            Map.entry(201, "Created"),
            Map.entry(202, "Accepted"),
            Map.entry(203, "Non-Authoritative Information"),
            Map.entry(204, "No Content"),
            Map.entry(205, "Reset Content"),
            Map.entry(206, "Partial Content"),
            Map.entry(300, "Multiple Choices"),
            Map.entry(301, "Moved Permanently"),
            Map.entry(302, "Found"),
            Map.entry(303, "See Other"),
            Map.entry(304, "Not Modified"),
            Map.entry(307, "Temporary Redirect"),
            Map.entry(308, "Permanent Redirect"),
            Map.entry(400, "Bad Request"),
            Map.entry(401, "Unauthorized"),
            Map.entry(402, "Payment Required"),
            Map.entry(403, "Forbidden"),
            Map.entry(404, "Not Found"),
            Map.entry(405, "Method Not Allowed"),
            Map.entry(406, "Not Acceptable"),
            Map.entry(407, "Proxy Authentication Required"),
            Map.entry(408, "Request Timeout"),
            Map.entry(409, "Conflict"),
            Map.entry(410, "Gone"),
            Map.entry(411, "Length Required"),
            Map.entry(412, "Precondition Failed"),
            Map.entry(413, "Content Too Large"),
            Map.entry(414, "URI Too Long"),
            Map.entry(415, "Unsupported Media Type"),
            Map.entry(416, "Range Not Satisfiable"),
            Map.entry(417, "Expectation Failed"),
            Map.entry(422, "Unprocessable Content"),
            Map.entry(429, "Too Many Requests"),
            Map.entry(500, "Internal Server Error"),
            Map.entry(501, "Not Implemented"),
            Map.entry(502, "Bad Gateway"),
            Map.entry(503, "Service Unavailable"),
            Map.entry(504, "Gateway Timeout"),
            Map.entry(505, "HTTP Version Not Supported"));

    private HttpStatusText() {
    }

    /**
     * Returns the standard reason phrase for a status code.
     *
     * @param statusCode the status code
     * @return the phrase, or an empty string for an unknown code
     */
    public static String of(int statusCode) {
        return PHRASES.getOrDefault(statusCode, "");
    }
}
