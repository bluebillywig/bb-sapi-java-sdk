package com.bluebillywig.sapi.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.util.Hotp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RpcTokenAuthenticatorTest {
    @Test
    void returnsRpctokenHeaderInCorrectFormat() {
        RpcTokenAuthenticator authenticator = new RpcTokenAuthenticator(1, "some-secret");

        Map<String, String> headers = authenticator.authenticate();

        assertTrue(headers.containsKey("rpctoken"));
        String[] parts = headers.get("rpctoken").split("-");
        assertEquals("1", parts[0]);
        assertFalse(parts[1].isEmpty());

        long now = Instant.now().getEpochSecond();
        Map<Integer, String> validTokens = Hotp.generateByTimeWindow("some-secret", 120, -1, 1, now);
        assertTrue(validTokens.containsValue(parts[1]));
    }

    @Test
    void onlyReturnsTheRpctokenHeader() {
        Map<String, String> headers = new RpcTokenAuthenticator(1, "some-secret").authenticate();

        assertEquals(List.of("rpctoken"), new ArrayList<>(headers.keySet()));
    }

    @Test
    void emptyAuthenticatorReturnsNoHeaders() {
        assertTrue(new EmptyAuthenticator().authenticate().isEmpty());
    }
}
