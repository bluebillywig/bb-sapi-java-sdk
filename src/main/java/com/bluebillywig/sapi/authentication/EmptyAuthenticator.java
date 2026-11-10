package com.bluebillywig.sapi.authentication;

import java.util.Map;

/** An authenticator that performs no authentication (pass-through). Useful for testing. */
public final class EmptyAuthenticator implements Authenticator {
    @Override
    public Map<String, String> authenticate() {
        return Map.of();
    }
}
