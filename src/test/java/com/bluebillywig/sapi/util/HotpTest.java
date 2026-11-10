package com.bluebillywig.sapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HotpTest {
    @Test
    void consistentForSameInputs() {
        assertEquals(Hotp.generateByCounter("test-key", 42), Hotp.generateByCounter("test-key", 42));
    }

    @Test
    void differentForDifferentCounters() {
        assertNotEquals(Hotp.generateByCounter("test-key", 1), Hotp.generateByCounter("test-key", 2));
    }

    @Test
    void generates40CharHex() {
        assertTrue(Hotp.generateByCounter("my-secret", 100).matches("^[0-9a-f]{40}$"));
    }

    @Test
    void encodesNonAsciiSecretsAsUtf8() {
        // Golden vector shared with the Node SDK (regression guard for its PR #2 ascii->utf8 fix).
        // "sëcrét-café-🔑" written with unicode escapes so the source encoding cannot matter.
        String token = Hotp.generateByCounter("sëcrét-café-🔑", 42);

        assertEquals("dc631268d43d625047ec064f8579148d3d7da25e", token);
        assertNotEquals("c4e7e1a05ab993c2ed5f21bfbe535e634092ef50", token);
    }

    @Test
    void generatesByTimeWithExplicitTimestamp() {
        // counter = floor(1000 / 120) = 8
        assertEquals(Hotp.generateByCounter("my-secret", 8), Hotp.generateByTime("my-secret", 120, 1000));
    }

    @Test
    void timeWindowDefaultsToThreeShifts() {
        Map<Integer, String> tokens = Hotp.generateByTimeWindow("my-secret", 120);
        assertEquals(3, tokens.size());
        assertTrue(tokens.containsKey(-1));
        assertTrue(tokens.containsKey(0));
        assertTrue(tokens.containsKey(1));
    }

    @Test
    void timeWindowTokensMatchCounters() {
        Map<Integer, String> tokens = Hotp.generateByTimeWindow("my-secret", 120, -1, 1, 1000);
        final int counter = 8;

        assertEquals(3, tokens.size());
        assertEquals(Hotp.generateByCounter("my-secret", counter - 1), tokens.get(-1));
        assertEquals(Hotp.generateByCounter("my-secret", counter), tokens.get(0));
        assertEquals(Hotp.generateByCounter("my-secret", counter + 1), tokens.get(1));
    }

    // Beyond the .NET suite: guards for the places where the JDK differs from .NET.

    @Test
    void matchesAnIndependentHmacSha1Implementation() {
        // Digests computed with Python's hmac module over struct.pack(">Q", counter).
        assertEquals("381630fb79ccb67167e03a92a56cb96485d6f958", Hotp.generateByCounter("test-key", 42));
        assertEquals("78290d7a38bb7ef2f950f8332018dc7880e3db10", Hotp.generateByCounter("my-secret", 100));
    }

    @Test
    void emptySecretIsAccepted() {
        // The JDK rejects an empty key; HMAC zero-pads keys, so the result must be the .NET one.
        assertEquals("eeb00b0bcc864679ff2d8dd30bec495cb5f2ee9e", Hotp.generateByCounter("", 1));
    }

    @Test
    void nonPositiveWindowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Hotp.generateByTime("k", 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> Hotp.generateByTime("k", -5, 1000));
    }

    @Test
    void negativeTimestampsFloorLikeTheDotNetImplementation() {
        // floor(-1 / 120) = -1, not 0
        assertEquals(Hotp.generateByCounter("k", -1), Hotp.generateByTime("k", 120, -1));
        assertEquals("548dac016bd085c43ba50df6b2c284edea4c3f25", Hotp.generateByTime("k", 120, -1));
    }
}
