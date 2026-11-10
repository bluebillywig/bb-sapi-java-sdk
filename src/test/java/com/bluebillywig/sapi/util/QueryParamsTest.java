package com.bluebillywig.sapi.util;

import static com.bluebillywig.sapi.util.QueryParams.param;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryParamsTest {
    @Test
    void stringsAsIs() {
        assertEquals(Map.of("key", "value"), QueryParams.build(param("key", "value")));
    }

    @Test
    void numbersToStrings() {
        assertEquals(Map.of("limit", "15", "offset", "0"), QueryParams.build(param("limit", 15), param("offset", 0)));
    }

    @Test
    void booleansToTrueFalse() {
        assertEquals(Map.of("active", "true", "deleted", "false"), QueryParams.build(param("active", true), param("deleted", false)));
    }

    @Test
    void omitsNulls() {
        assertEquals(Map.of("key", "value"), QueryParams.build(param("key", "value"), param("empty", null)));
    }

    @Test
    void mixedTypes() {
        Map<String, String> query = QueryParams.build(
                param("name", "test"), param("count", 42), param("active", true), param("missing", null), param("ratio", 2.5));
        assertEquals(Map.of("name", "test", "count", "42", "active", "true", "ratio", "2.5"), query);
    }

    @Test
    void emptyForAllNull() {
        assertTrue(QueryParams.build(param("a", null), param("b", null)).isEmpty());
    }

    @Test
    void appendKeepsExistingParamsVerbatimAndReplacesSameKey() {
        Map<String, String> query = new java.util.LinkedHashMap<>();
        query.put("partNumber", "2");
        query.put("sort", "createddate desc");

        String url = QueryParams.append("https://s3.example.com/u?X-Amz-Credential=a%2Fb&partNumber=1", query);

        assertEquals("https://s3.example.com/u?X-Amz-Credential=a%2Fb&partNumber=2&sort=createddate%20desc", url);
    }

    @Test
    void getDecodesPlusAndPercent() {
        assertEquals("createddate desc", QueryParams.get("https://x/?sort=createddate+desc", "sort"));
        assertEquals("a/b", QueryParams.get("https://x/?k=a%2Fb", "k"));
        assertNull(QueryParams.get("https://x/?k=1", "missing"));
    }

    // Beyond the .NET suite: byte-for-byte parity of the encoder, and the iteration-order contract.

    @Test
    void encodesLikeUriEscapeDataString() {
        // Unreserved characters stay; everything else is UTF-8 percent-encoded with uppercase hex;
        // a space is %20, never '+'.
        assertEquals("AZaz09-_.~", QueryParams.encode("AZaz09-_.~"));
        assertEquals("a%20b%2Bc%2Fd%3De%26f", QueryParams.encode("a b+c/d=e&f"));
        assertEquals("fq%5B0%5D", QueryParams.encode("fq[0]"));
        assertEquals("%2A%21%27%28%29", QueryParams.encode("*!'()"));
        assertEquals("%C3%AB%F0%9F%94%91", QueryParams.encode("ë🔑"));
        assertEquals("%22published%22%3A", QueryParams.encode("\"published\":"));
    }

    @Test
    void appendEncodesKeysAndValuesAndFragments() {
        Map<String, String> query = new java.util.LinkedHashMap<>();
        query.put("fq[0]", "statusSort:\"published\"");
        query.put("sort", "createddate desc");
        assertEquals("https://x/p?fq%5B0%5D=statusSort%3A%22published%22&sort=createddate%20desc#frag",
                QueryParams.append("https://x/p#frag", query));
        assertEquals("https://x/p", QueryParams.append("https://x/p", Map.of()));
    }

    @Test
    void appendDropsDuplicatesOfAReplacedKey() {
        assertEquals("https://x/?a=1&b=9", QueryParams.append("https://x/?a=1&b=2&b=3", Map.of("b", "9")));
    }

    @Test
    void decodeIsLenientAboutMalformedEscapes() {
        assertEquals("100%", QueryParams.decode("100%"));
        assertEquals("%zz", QueryParams.decode("%zz"));
        assertEquals("ë", QueryParams.decode("%C3%AB"));
        assertEquals("%C3", QueryParams.decode("%C3"));
        assertEquals("a b", QueryParams.decode("a+b"));
    }

    @Test
    void formatsNumbersWithTheInvariantCulture() {
        assertEquals("2", QueryParams.format(2.0));
        assertEquals("2.5", QueryParams.format(2.5f));
        assertEquals("10000000", QueryParams.format(1.0E7));
        assertEquals("1.50", QueryParams.format(new java.math.BigDecimal("1.50")));
        assertEquals("42", QueryParams.format(42L));
        assertEquals("a", QueryParams.format('a'));
        assertEquals(List.of("true", "false"), List.of(QueryParams.format(true), QueryParams.format(false)));
    }
}
