package com.bluebillywig.sapi.authentication;

import java.util.Map;

/** Strategy interface for authenticating outgoing SAPI requests. */
public interface Authenticator {
    /**
     * Returns headers to add to the outgoing request for authentication.
     *
     * @return the authentication headers
     */
    Map<String, String> authenticate();
}
