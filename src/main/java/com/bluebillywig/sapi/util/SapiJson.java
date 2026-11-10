package com.bluebillywig.sapi.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.UncheckedIOException;

/**
 * Shared JSON configuration for SAPI request and response bodies: null properties omitted when
 * writing (null map values are kept), property names matched case-insensitively and unknown
 * properties ignored when reading, trailing tokens rejected, and no escaping beyond what JSON
 * requires, so bodies stay readable on the wire.
 *
 * <p>Property names are explicit ({@code @JsonProperty}) on the SDK's own types, which is what the
 * .NET SDK's camelCase naming policy and {@code JsonPropertyName} attributes achieve there; plain
 * Java beans and maps you pass as a body keep the usual Jackson (lowerCamelCase) names.
 *
 * <p>Jackson's {@link JsonProcessingException} is checked; the helpers here rethrow it as an
 * {@link UncheckedIOException} whose cause is the original exception.
 */
public final class SapiJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS))
            .build();

    private SapiJson() {
    }

    /** @return the shared, configured mapper */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Serializes a value as compact JSON.
     *
     * @param value the value
     * @return the JSON text
     * @throws UncheckedIOException when the value cannot be serialized
     */
    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses JSON text as a tree.
     *
     * @param json the JSON text
     * @return the tree
     * @throws UncheckedIOException when the text is not valid JSON
     */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deserializes JSON text.
     *
     * @param json the JSON text
     * @param type the target type
     * @param <T> the target type
     * @return the value
     * @throws UncheckedIOException when the text is not valid JSON for the type
     */
    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deserializes JSON text into a generic type.
     *
     * @param json the JSON text
     * @param type the target type
     * @param <T> the target type
     * @return the value
     * @throws UncheckedIOException when the text is not valid JSON for the type
     */
    public static <T> T read(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
