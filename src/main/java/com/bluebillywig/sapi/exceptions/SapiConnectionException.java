package com.bluebillywig.sapi.exceptions;

/**
 * Exception for transport-level failures (network error, DNS failure, connection reset, or
 * request timeout) where no HTTP response was received.
 * {@link SapiRequestException#getStatusCode()} is 0 to signal "no response reached us"; the
 * underlying error is preserved as the {@linkplain #getCause() cause}.
 */
public class SapiConnectionException extends SapiRequestException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new connection exception.
     *
     * @param message the message
     */
    public SapiConnectionException(String message) {
        this(message, null);
    }

    /**
     * Creates a new connection exception.
     *
     * @param message the message
     * @param cause the underlying error, or {@code null}
     */
    public SapiConnectionException(String message, Throwable cause) {
        super(message, 0, null, cause);
    }
}
