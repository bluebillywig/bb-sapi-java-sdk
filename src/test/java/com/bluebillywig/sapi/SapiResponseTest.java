package com.bluebillywig.sapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.exceptions.SapiClientErrorException;
import com.bluebillywig.sapi.exceptions.SapiRequestException;
import com.bluebillywig.sapi.exceptions.SapiServerErrorException;
import com.bluebillywig.sapi.util.HttpStatusCodeCategory;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SapiResponseTest {
    private static SapiResponse make(int status) {
        return make(status, "", Map.of());
    }

    private static SapiResponse make(int status, String body) {
        return make(status, body, Map.of());
    }

    private static SapiResponse make(int status, String body, Map<String, String> headers) {
        return new SapiResponse("https://www.bluebillywig.com/", "GET", status, "", headers, body);
    }

    @Test
    void okForAll2xx() {
        for (int code = 200; code <= 299; code++) {
            assertTrue(make(code).isOk());
        }
    }

    @ParameterizedTest
    @CsvSource({"100,199", "300,399", "400,499", "500,599"})
    void notOkOutside2xx(int start, int end) {
        for (int code = start; code <= end; code++) {
            assertFalse(make(code).isOk());
        }
    }

    @Test
    void assertOkDoesNotThrowFor2xx() {
        for (int code = 200; code <= 299; code++) {
            make(code).assertOk();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "100,199,SapiRequestException",
        "300,399,SapiRequestException",
        "400,499,SapiClientErrorException",
        "500,599,SapiServerErrorException"})
    void assertOkThrowsTypedExceptions(int start, int end, String expected) {
        for (int code = start; code <= end; code++) {
            final int status = code;
            SapiRequestException ex = assertThrows(SapiRequestException.class, () -> make(status).assertOk());
            assertEquals(expected, ex.getClass().getSimpleName());
            assertEquals(code, ex.getStatusCode());
        }
    }

    @Test
    void exceptionCarriesBodyWhenNonEmpty() {
        SapiClientErrorException ex = assertThrows(SapiClientErrorException.class, () -> make(400, "error details").assertOk());
        assertEquals("error details", ex.getResponseBody());
    }

    @Test
    void exceptionBodyNullWhenEmpty() {
        SapiClientErrorException ex = assertThrows(SapiClientErrorException.class, () -> make(400).assertOk());
        assertNull(ex.getResponseBody());
    }

    @ParameterizedTest
    @CsvSource({
        "100,199,INFORMATIONAL",
        "200,299,SUCCESSFUL",
        "300,399,REDIRECTION",
        "400,499,CLIENT_ERROR",
        "500,599,SERVER_ERROR"})
    void statusCategory(int start, int end, HttpStatusCodeCategory expected) {
        for (int code = start; code <= end; code++) {
            assertEquals(expected, make(code).getStatusCategory());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 99, 600})
    void outOfRangeStatusThrows(int code) {
        assertThrows(IllegalArgumentException.class, () -> HttpStatusCodeCategory.of(code));
    }

    @Test
    void allOk() {
        assertTrue(SapiResponse.allOk(List.of(make(200), make(201), make(204))));
        assertFalse(SapiResponse.allOk(List.of(make(200), make(200), make(404), make(200))));
    }

    @Test
    void assertAllOk() {
        SapiResponse.assertAllOk(List.of(make(200), make(201)));
        assertThrows(SapiClientErrorException.class, () -> SapiResponse.assertAllOk(List.of(make(200), make(404), make(200))));
    }

    @Test
    void failedResponsesYieldsOnlyFailures() {
        List<SapiResponse> failed = SapiResponse.failedResponses(
                List.of(make(200), make(200), make(404), make(200), make(500), make(200)));
        assertEquals(2, failed.size());
    }

    @Test
    void exposesRawBody() {
        assertEquals("hello world", make(200, "hello world").getBody());
    }

    @Test
    void parsesJsonBody() {
        ObjectNode data = SapiJson.mapper().createObjectNode();
        data.putObject("object1").put("field1", "value1").put("field2", "value2");
        ObjectNode object2 = data.putObject("object2");
        object2.putObject("object3").put("field3", "value3");
        object2.putArray("list1").add("listValue1").add("listValue2");
        SapiResponse response = make(200, data.toString());

        assertEquals(data, response.json());
        assertEquals("value1", response.json(new TypeReference<Map<String, JsonNode>>() { }).get("object1").get("field1").asText());
    }

    @Test
    void jsonNullForEmptyBody() {
        assertNull(make(200).json());
        assertNull(make(200).json(new TypeReference<Map<String, String>>() { }));
    }

    @Test
    void jsonThrowsForNonJson() {
        UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> make(200, "some incorrect value").json());
        assertTrue(ex.getCause() instanceof JsonProcessingException);
    }

    @Test
    void headerIsCaseInsensitive() {
        SapiResponse response = make(200, "", Map.of("Content-Type", "application/json"));
        assertEquals("application/json", response.header("content-type"));
        assertEquals("application/json", response.header("Content-Type"));
        assertNull(response.header("X-Missing"));
    }

    @Test
    void queryParamFromUrl() {
        SapiResponse response = new SapiResponse("https://example.com/path?foo=bar&baz=qux", "GET", 200, "", Map.of(), "");
        assertEquals("bar", response.queryParam("foo"));
        assertEquals("qux", response.queryParam("baz"));
        assertNull(response.queryParam("missing"));
    }

    // Beyond the .NET suite.

    @Test
    void jsonRejectsTrailingGarbage() {
        assertThrows(UncheckedIOException.class, () -> make(200, "{} x").json());
    }
}
