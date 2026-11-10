package com.bluebillywig.sapi.exceptions;

/** Exception for 5xx (server error) HTTP responses. */
public class SapiServerErrorException extends SapiRequestException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new server-error exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code
     */
    public SapiServerErrorException(String message, int statusCode) {
        super(message, statusCode, null);
    }

    /**
     * Creates a new server-error exception.
     *
     * @param message the message
     * @param statusCode the HTTP status code
     * @param responseBody the raw response body, or {@code null}
     */
    public SapiServerErrorException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }
}
