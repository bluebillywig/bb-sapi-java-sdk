package com.bluebillywig.sapi.util;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Query-string helpers. Encoding follows RFC 3986 (only unreserved characters stay literal, a
 * space is {@code %20}, never {@code +}), byte-for-byte what the .NET SDK produces.
 */
public final class QueryParams {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private QueryParams() {
    }

    /**
     * One query entry for {@link #build(Param...)}; a {@code null} value is omitted.
     *
     * @param key the parameter name
     * @param value the value, or {@code null}
     */
    public record Param(String key, Object value) {
    }

    /**
     * Creates a query entry for {@link #build(Param...)}.
     *
     * @param key the parameter name
     * @param value the value, or {@code null}
     * @return the entry
     */
    public static Param param(String key, Object value) {
        return new Param(key, value);
    }

    /**
     * Builds a query parameter map (in insertion order), omitting entries whose value is
     * {@code null}. Booleans become {@code "true"}/{@code "false"}; numbers are formatted with
     * the invariant culture; everything else uses {@link Object#toString()}.
     *
     * @param entries the entries
     * @return the map
     */
    public static Map<String, String> build(Param... entries) {
        Map<String, String> query = new LinkedHashMap<>();
        for (Param entry : entries) {
            String formatted = format(entry.value());
            if (formatted != null) {
                query.put(entry.key(), formatted);
            }
        }
        return query;
    }

    /**
     * Formats a single query value.
     *
     * @param value the value
     * @return the text, or {@code null} when the value is {@code null}
     */
    public static String format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return Double.toString(d);
            }
            // Shortest round-trip digits without a trailing ".0" or exponent, as the invariant
            // culture prints them (2.0 -> "2", 2.5 -> "2.5").
            String text = value instanceof Float ? Float.toString((Float) value) : Double.toString(d);
            BigDecimal plain = new BigDecimal(text);
            return plain.signum() == 0 ? "0" : plain.stripTrailingZeros().toPlainString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return value.toString();
    }

    /**
     * Appends (or replaces, by key) query parameters on a URL. Existing parameters are kept
     * byte-for-byte; only a same-named parameter is replaced. Mirrors {@code URLSearchParams.set}.
     *
     * @param url the URL
     * @param query the parameters to set, in order
     * @return the URL with the parameters applied
     */
    public static String append(String url, Map<String, String> query) {
        int hashIndex = url.indexOf('#');
        String fragment = hashIndex >= 0 ? url.substring(hashIndex) : "";
        String withoutFragment = hashIndex >= 0 ? url.substring(0, hashIndex) : url;

        int questionIndex = withoutFragment.indexOf('?');
        String baseUrl = questionIndex >= 0 ? withoutFragment.substring(0, questionIndex) : withoutFragment;
        String existing = questionIndex >= 0 ? withoutFragment.substring(questionIndex + 1) : "";

        List<Pair> pairs = new ArrayList<>();
        for (String raw : splitPairs(existing)) {
            int eq = raw.indexOf('=');
            String rawKey = eq >= 0 ? raw.substring(0, eq) : raw;
            pairs.add(new Pair(decode(rawKey), raw));
        }

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String encoded = encode(key) + "=" + encode(entry.getValue());
            int index = -1;
            for (int i = 0; i < pairs.size(); i++) {
                if (pairs.get(i).decodedKey().equals(key)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                pairs.set(index, new Pair(key, encoded));
                pairs.removeIf(p -> p.decodedKey().equals(key) && !p.raw().equals(encoded));
            } else {
                pairs.add(new Pair(key, encoded));
            }
        }

        if (pairs.isEmpty()) {
            return baseUrl + fragment;
        }

        StringBuilder sb = new StringBuilder(baseUrl).append('?');
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append(pairs.get(i).raw());
        }
        return sb.append(fragment).toString();
    }

    /**
     * Returns the first value of a query parameter in a URL.
     *
     * @param url the URL
     * @param name the parameter name
     * @return the decoded value, or {@code null} when the parameter is not present
     */
    public static String get(String url, String name) {
        int hashIndex = url.indexOf('#');
        String withoutFragment = hashIndex >= 0 ? url.substring(0, hashIndex) : url;
        int questionIndex = withoutFragment.indexOf('?');
        if (questionIndex < 0) {
            return null;
        }

        for (String raw : splitPairs(withoutFragment.substring(questionIndex + 1))) {
            int eq = raw.indexOf('=');
            String key = decode(eq >= 0 ? raw.substring(0, eq) : raw);
            if (key.equals(name)) {
                return eq >= 0 ? decode(raw.substring(eq + 1)) : "";
            }
        }
        return null;
    }

    /**
     * Decodes a query component, treating {@code +} as a space like {@code URLSearchParams}.
     * Malformed escapes are left as they are rather than rejected.
     *
     * @param component the encoded component
     * @return the decoded text
     */
    public static String decode(String component) {
        return unescape(component.replace('+', ' '));
    }

    /**
     * Percent-encodes a string as one URL component (RFC 3986: everything except
     * {@code A-Z a-z 0-9 - _ . ~} becomes UTF-8 {@code %XX} with uppercase hex).
     *
     * @param value the text
     * @return the encoded text
     */
    public static String encode(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(HEX[c >> 4]).append(HEX[c & 0x0F]);
            }
        }
        return sb.toString();
    }

    private record Pair(String decodedKey, String raw) {
    }

    private static List<String> splitPairs(String query) {
        List<String> parts = new ArrayList<>();
        for (String part : query.split("&")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts;
    }

    private static String unescape(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '%' && isEscape(s, i)) {
                int start = i;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while (i < n && s.charAt(i) == '%' && isEscape(s, i)) {
                    bytes.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                    i += 3;
                }
                try {
                    sb.append(StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes.toByteArray())));
                } catch (CharacterCodingException e) {
                    sb.append(s, start, i);
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isEscape(String s, int i) {
        return i + 2 < s.length()
                && Character.digit(s.charAt(i + 1), 16) >= 0
                && Character.digit(s.charAt(i + 2), 16) >= 0;
    }
}
