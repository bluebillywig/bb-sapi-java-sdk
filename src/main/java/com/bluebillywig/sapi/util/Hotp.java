package com.bluebillywig.sapi.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HOTP token generation as used by SAPI RPC tokens: a full HMAC-SHA1 digest (40-char lowercase
 * hex) over an 8-byte big-endian counter, keyed with the UTF-8 bytes of the shared secret.
 */
public final class Hotp {
    private Hotp() {
    }

    /**
     * Generates a full HMAC-SHA1 HOTP token (40-char hex) for a given counter.
     * Unlike standard HOTP which truncates to 6-8 digits, this returns the full digest.
     *
     * @param key the shared secret
     * @param counter the counter
     * @return the 40-char lowercase hex token
     */
    public static String generateByCounter(String key, long counter) {
        Objects.requireNonNull(key, "key");

        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        // The secret is HMAC'd over its UTF-8 bytes (not ASCII): a non-ASCII secret must
        // produce the same digest SAPI computes.
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 0) {
            // SecretKeySpec rejects an empty key; HMAC zero-pads the key to the block size, so a
            // single zero byte is the equivalent key (and what an empty key means in .NET).
            keyBytes = new byte[] {0};
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(counterBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is not available", e);
        }
    }

    /**
     * Generates an HOTP token based on the current time and a window size.
     *
     * @param key the shared secret key
     * @param window time window in seconds (e.g. 120 for 2-minute windows)
     * @return the token
     */
    public static String generateByTime(String key, int window) {
        return generateByTime(key, window, Instant.now().getEpochSecond());
    }

    /**
     * Generates an HOTP token based on a timestamp and window size. The counter is derived as
     * {@code floor(timestamp / window)}. Client and server clocks must be within the window for
     * tokens to match.
     *
     * @param key the shared secret key
     * @param window time window in seconds (e.g. 120 for 2-minute windows)
     * @param timestamp Unix timestamp in seconds
     * @return the token
     */
    public static String generateByTime(String key, int window, long timestamp) {
        return generateByCounter(key, floorDiv(timestamp, window));
    }

    /**
     * Generates HOTP tokens for the windows -1, 0 and +1 around the current counter.
     *
     * @param key the shared secret key
     * @param window time window in seconds
     * @return a map from shift offset to hex HMAC string, in ascending shift order
     */
    public static Map<Integer, String> generateByTimeWindow(String key, int window) {
        return generateByTimeWindow(key, window, -1, 1);
    }

    /**
     * Generates HOTP tokens for a range of time windows around the current counter.
     *
     * @param key the shared secret key
     * @param window time window in seconds
     * @param min the lowest shift
     * @param max the highest shift
     * @return a map from shift offset to hex HMAC string, in ascending shift order
     */
    public static Map<Integer, String> generateByTimeWindow(String key, int window, int min, int max) {
        return generateByTimeWindow(key, window, min, max, Instant.now().getEpochSecond());
    }

    /**
     * Generates HOTP tokens for a range of time windows around the counter of a timestamp. Useful
     * for server-side validation to account for minor clock drift.
     *
     * @param key the shared secret key
     * @param window time window in seconds
     * @param min the lowest shift
     * @param max the highest shift
     * @param timestamp Unix timestamp in seconds
     * @return a map from shift offset to hex HMAC string, in ascending shift order
     */
    public static Map<Integer, String> generateByTimeWindow(String key, int window, int min, int max, long timestamp) {
        long counter = floorDiv(timestamp, window);
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int shift = min; shift <= max; shift++) {
            result.put(shift, generateByCounter(key, counter + shift));
        }
        return result;
    }

    private static long floorDiv(long value, long divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Window must be positive: " + divisor);
        }
        return Math.floorDiv(value, divisor);
    }
}
