package com.bluebillywig.sapi.util;

/** The class of an HTTP status code. */
public enum HttpStatusCodeCategory {
    /** 1xx. */
    INFORMATIONAL,
    /** 2xx. */
    SUCCESSFUL,
    /** 3xx. */
    REDIRECTION,
    /** 4xx. */
    CLIENT_ERROR,
    /** 5xx. */
    SERVER_ERROR;

    /**
     * Returns the category of a status code.
     *
     * @param statusCode the status code
     * @return the category
     * @throws IllegalArgumentException when the status code is outside 100-599
     */
    public static HttpStatusCodeCategory of(int statusCode) {
        if (statusCode >= 100 && statusCode <= 199) {
            return INFORMATIONAL;
        }
        if (statusCode >= 200 && statusCode <= 299) {
            return SUCCESSFUL;
        }
        if (statusCode >= 300 && statusCode <= 399) {
            return REDIRECTION;
        }
        if (statusCode >= 400 && statusCode <= 499) {
            return CLIENT_ERROR;
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return SERVER_ERROR;
        }
        throw new IllegalArgumentException("Unexpected HTTP status code: " + statusCode);
    }
}
