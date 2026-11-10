package com.bluebillywig.sapi.authentication;

import com.bluebillywig.sapi.util.Hotp;
import java.util.Map;
import java.util.Objects;

/**
 * Authenticates requests with a time-based HOTP RPC token, sent as the {@code rpctoken} header
 * in the form {@code {tokenId}-{token}}.
 */
public final class RpcTokenAuthenticator implements Authenticator {
    /**
     * Default token expiration window in seconds. Tokens are valid within this time window.
     * Both client and server must have reasonably synchronized clocks (within this window)
     * for authentication to succeed.
     */
    public static final int DEFAULT_TOKEN_EXPIRATION_SECONDS = 120;

    private final String sharedSecret;
    private final int tokenId;
    private final int tokenExpiration;

    /**
     * Creates an authenticator for the given token ID and shared secret.
     *
     * @param tokenId the RPC token ID
     * @param sharedSecret the shared secret
     */
    public RpcTokenAuthenticator(int tokenId, String sharedSecret) {
        this(tokenId, sharedSecret, DEFAULT_TOKEN_EXPIRATION_SECONDS);
    }

    /**
     * Creates an authenticator for the given token ID and shared secret.
     *
     * @param tokenId the RPC token ID
     * @param sharedSecret the shared secret
     * @param tokenExpiration the token expiration window in seconds
     */
    public RpcTokenAuthenticator(int tokenId, String sharedSecret, int tokenExpiration) {
        this.tokenId = tokenId;
        this.sharedSecret = Objects.requireNonNull(sharedSecret, "sharedSecret");
        this.tokenExpiration = tokenExpiration;
    }

    /** @return the RPC token ID */
    public int getTokenId() {
        return tokenId;
    }

    /** @return the token expiration window in seconds */
    public int getTokenExpiration() {
        return tokenExpiration;
    }

    @Override
    public Map<String, String> authenticate() {
        String token = Hotp.generateByTime(sharedSecret, tokenExpiration);
        return Map.of("rpctoken", tokenId + "-" + token);
    }
}
