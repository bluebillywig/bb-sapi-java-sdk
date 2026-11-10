package com.bluebillywig.sapi.exceptions;

/** Exception for 4xx (client error) HTTP responses. */
public class SapiClientErrorException extends SapiRequestException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new client-error exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code
     */
    public SapiClientErrorException(String message, int statusCode) {
        super(message, statusCode, null);
    }

    /**
     * Creates a new client-error exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code
     * @param responseBody the raw response body, or {@code null}
     */
    public SapiClientErrorException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }
}
