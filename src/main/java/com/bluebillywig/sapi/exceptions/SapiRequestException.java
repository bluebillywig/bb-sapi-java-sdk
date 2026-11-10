package com.bluebillywig.sapi.exceptions;

/**
 * Base exception for non-successful HTTP responses from the SAPI.
 * (Named {@code HTTPRequestException} in the Node SDK.)
 */
public class SapiRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** The HTTP status code. */
    private final int statusCode;

    /** The response body, or {@code null}. */
    private final String responseBody;

    /**
     * Creates a new request exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code, or 0 when no response was received
     */
    public SapiRequestException(String message, int statusCode) {
        this(message, statusCode, null, null);
    }

    /**
     * Creates a new request exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code, or 0 when no response was received
     * @param responseBody the raw response body, or {@code null} when empty or absent
     */
    public SapiRequestException(String message, int statusCode, String responseBody) {
        this(message, statusCode, responseBody, null);
    }

    /**
     * Creates a new request exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code, or 0 when no response was received
     * @param responseBody the raw response body, or {@code null} when empty or absent
     * @param cause the underlying cause, or {@code null}
     */
    public SapiRequestException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /** @return the HTTP status code, or 0 when no response was received */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the raw response body, or {@code null} when empty or absent */
    public String getResponseBody() {
        return responseBody;
    }
}
