package com.bluebillywig.sapi.search;

import com.bluebillywig.sapi.util.QueryParams;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The filterset: the structure the OVP builds in its filter UI, and the shape SAPI's
 * {@code filterset} parameter takes.
 *
 * <p>Deliberately NOT compiled to a Solr query here. SAPI compiles filtersets itself, using the
 * same SearchRequestHelper that serves the OVP, so compiling client-side would be a second
 * implementation of semantics the server owns, free to drift, with a failure mode that is
 * invisible: a filter SAPI cannot read is ignored, and the response is HTTP 200 with neither
 * {@code numfound} nor {@code items}, which reads exactly like an empty library.
 *
 * <p>Mirrors {@code app/services/filter-set.types.ts} in OVP6, so a filterset moves between the
 * UI, the API and any SDK unchanged.
 *
 * <p>Server-side quirks a caller inherits (the compiler is formatengine's):
 * <ul>
 * <li>A filter whose value is the string {@code '0'} is dropped by the backend's empty-value
 * guard, so "views is 0" cannot be expressed as a filterset.</li>
 * <li>In values, {@code +} becomes a space and {@code "} is stripped before compilation.</li>
 * <li>An unknown FIELD is not an error: it queries a non-existent index field and returns
 * numfound=0, so a typo'd field name looks like an empty library.</li>
 * </ul>
 *
 * <pre>{@code
 * FilterSet filterSet = FilterSet.create()
 *     .where("status", FilterOperator.IS, "published")
 *     .where("title", FilterOperator.CONTAINS, "koert");
 *
 * sdk.mediaClip().search(filterSet);
 * }</pre>
 *
 * <p>Instances are immutable.
 */
@JsonSerialize(using = FilterSet.WireSerializer.class)
@JsonDeserialize(using = FilterSet.WireDeserializer.class)
public final class FilterSet {
    private final List<FilterGroup> groups;

    private FilterSet(List<FilterGroup> groups) {
        this.groups = groups;
    }

    /** @return an empty filterset */
    public static FilterSet create() {
        return new FilterSet(List.of());
    }

    /**
     * Creates a filterset from groups.
     *
     * @param groups the groups, or {@code null} for none
     * @return the filterset
     */
    public static FilterSet from(Iterable<FilterGroup> groups) {
        List<FilterGroup> copy = new ArrayList<>();
        if (groups != null) {
            groups.forEach(copy::add);
        }
        return new FilterSet(Collections.unmodifiableList(copy));
    }

    /**
     * Ingests a filterset from JSON: either a bare list of groups or the {@code SearchRequest}
     * envelope OVP6 sends ({@code {"type":"SearchRequest","filterSet":[...]}}). Malformed groups
     * (no {@code filters} array) and filters without a string {@code field} are skipped as junk
     * rather than crashing; an unknown operator throws a descriptive
     * {@link IllegalArgumentException}. Numbers keep their literal text.
     *
     * @param json the JSON text
     * @return the filterset
     * @throws UncheckedIOException when the text is not valid JSON
     */
    public static FilterSet from(String json) {
        Objects.requireNonNull(json, "json");
        try (JsonParser parser = SapiJson.mapper().createParser(json)) {
            if (parser.nextToken() == null) {
                throw new UncheckedIOException(new JsonParseException(parser, "No content to map due to end-of-input"));
            }
            Object tree = readTree(parser);
            if (parser.nextToken() != null) {
                throw new UncheckedIOException(new JsonParseException(parser, "Unexpected content after the JSON value"));
            }
            return ingest(tree);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Ingests a filterset from a parsed JSON node. See {@link #from(String)}.
     *
     * @param node the parsed node, or {@code null}
     * @return the filterset
     */
    public static FilterSet from(JsonNode node) {
        return ingest(nodeToTree(node));
    }

    // -- ingestion -------------------------------------------------------------------------

    /** A number carried as its JSON literal text, so it serialises exactly as ingested. */
    private record JsonNumber(String literal) {
        @Override
        public String toString() {
            return literal;
        }
    }

    private static Object readTree(JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        switch (token) {
            case START_OBJECT: {
                Map<String, Object> map = new LinkedHashMap<>();
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    parser.nextToken();
                    map.put(name, readTree(parser));
                }
                return map;
            }
            case START_ARRAY: {
                List<Object> list = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    list.add(readTree(parser));
                }
                return list;
            }
            case VALUE_STRING:
                return parser.getText();
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return new JsonNumber(parser.getText());
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static Object nodeToTree(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                map.put(field.getKey(), nodeToTree(field.getValue()));
            }
            return map;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(child -> list.add(nodeToTree(child)));
            return list;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber()) {
            return new JsonNumber(node.isBigDecimal() ? node.decimalValue().toString() : node.asText());
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return null;
    }

    private static FilterSet ingest(Object root) {
        List<?> groupsNode = null;
        if (root instanceof List<?> list) {
            groupsNode = list;
        } else if (root instanceof Map<?, ?> envelope && envelope.get("filterSet") instanceof List<?> list) {
            groupsNode = list;
        }

        List<FilterGroup> groups = new ArrayList<>();
        if (groupsNode == null) {
            return new FilterSet(groups);
        }

        for (Object groupNode : groupsNode) {
            // Ingested external data; a group without a filters array is junk, not a crash.
            if (!(groupNode instanceof Map<?, ?> groupObject) || !(groupObject.get("filters") instanceof List<?> filtersNode)) {
                continue;
            }

            List<Filter> filters = new ArrayList<>();
            for (Object filterNode : filtersNode) {
                if (!(filterNode instanceof Map<?, ?> filterObject)) {
                    continue;
                }
                if (!(filterObject.get("field") instanceof String field)) {
                    continue;
                }

                String operatorName = filterObject.get("operator") instanceof String s ? s : null;
                FilterOperator op = FilterOperator.parse(operatorName);

                String type = filterObject.get("type") instanceof String s ? s : null;

                filters.add(new Filter(field, op, jsonToValue(filterObject.get("value")), type));
            }
            groups.add(new FilterGroup(filters));
        }

        return new FilterSet(groups);
    }

    private static Object jsonToValue(Object node) {
        if (node instanceof List<?> list) {
            List<Object> values = new ArrayList<>();
            for (Object item : list) {
                values.add(jsonToScalar(item));
            }
            return values;
        }
        return jsonToScalar(node);
    }

    private static Object jsonToScalar(Object node) {
        if (node instanceof String || node instanceof Boolean || node instanceof JsonNumber) {
            return node;
        }
        return null;
    }

    // -- building --------------------------------------------------------------------------

    /**
     * Adds a presence condition as its own group, so it is AND-ed with the rest.
     *
     * @param field the field
     * @param operator the operator
     * @return a new filterset
     */
    public FilterSet where(String field, FilterOperator operator) {
        return andGroup(new Filter(field, operator));
    }

    /**
     * Adds a condition as its own group, so it is AND-ed with the rest.
     *
     * @param field the field
     * @param operator the operator
     * @param value a string, number, boolean, or a sequence of those
     * @return a new filterset
     */
    public FilterSet where(String field, FilterOperator operator, Object value) {
        return andGroup(new Filter(field, operator, value));
    }

    /**
     * Adds a condition as its own group, so it is AND-ed with the rest.
     *
     * @param field the field
     * @param operator the operator
     * @param value a string, number, boolean, or a sequence of those
     * @param type optional entity type constraint
     * @return a new filterset
     */
    public FilterSet where(String field, FilterOperator operator, Object value, String type) {
        return andGroup(new Filter(field, operator, value, type));
    }

    /**
     * Adds several conditions as one group, so they are OR-ed with each other.
     *
     * @param filters the filters of the group
     * @return a new filterset
     */
    public FilterSet andGroup(Filter... filters) {
        List<FilterGroup> next = new ArrayList<>(groups);
        next.add(new FilterGroup(filters));
        return new FilterSet(Collections.unmodifiableList(next));
    }

    // -- wire format -----------------------------------------------------------------------

    /**
     * The wire format: what SAPI's {@code filterset} parameter expects. Filters with nothing to
     * match on are dropped, values are normalised to strings, presence operators carry the
     * {@code *} placeholder the backend requires, and groups left empty are removed.
     *
     * @return the normalised groups
     */
    public List<FilterGroup> toArray() {
        List<FilterGroup> result = new ArrayList<>();
        for (FilterGroup group : groups) {
            List<Filter> filters = new ArrayList<>();
            for (Filter filter : group.getFilters()) {
                if (hasValue(filter)) {
                    filters.add(strip(filter));
                }
            }
            if (!filters.isEmpty()) {
                result.add(new FilterGroup(filters));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** @return the wire format as a JSON array node */
    public ArrayNode toJsonNode() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode array = factory.arrayNode();
        for (FilterGroup group : toArray()) {
            ArrayNode filters = factory.arrayNode();
            for (Filter filter : group.getFilters()) {
                ObjectNode obj = factory.objectNode();
                obj.put("field", filter.getField());
                obj.put("operator", filter.getOperator().toWire());
                if (filter.getValue() instanceof String scalar) {
                    obj.put("value", scalar);
                } else if (filter.getValue() instanceof List<?> list) {
                    ArrayNode values = obj.putArray("value");
                    for (Object item : list) {
                        values.add(String.valueOf(item));
                    }
                }
                if (filter.getType() != null && !filter.getType().isEmpty()) {
                    obj.put("type", filter.getType());
                }
                filters.add(obj);
            }
            ObjectNode groupNode = factory.objectNode();
            groupNode.set("filters", filters);
            array.add(groupNode);
        }
        return array;
    }

    /** @return the wire format as compact JSON */
    public String toJson() {
        return SapiJson.write(toJsonNode());
    }

    @Override
    public String toString() {
        return toJson();
    }

    /** @return whether nothing survives normalisation */
    public boolean isEmpty() {
        return toArray().isEmpty();
    }

    private static boolean hasValue(Filter filter) {
        if (filter.getOperator().isValueless()) {
            return true;
        }
        for (Object v : enumerateScalars(filter.getValue())) {
            if (v instanceof Boolean || v instanceof JsonNumber || v instanceof Number) {
                return true;
            }
            if (v instanceof String s && !isBlank(s)) {
                return true;
            }
        }
        return false;
    }

    /** Normalise to the wire shape: what the OVP sends and the backend can read. */
    private static Filter strip(Filter filter) {
        Object value;
        if (filter.getOperator().isValueless()) {
            // The backend's compiler skips ANY filter whose value is empty, presence tests
            // included, so isEmpty/isNotEmpty must carry a placeholder or they silently never
            // fire (verified live: a bare isEmpty returned the full unfiltered publication). '*'
            // is what OVP6 sends ("backend needs a value to work"), and it overrides whatever
            // the caller supplied.
            value = "*";
        } else if (filter.getValue() == null) {
            value = null;
        } else if (isScalar(filter.getValue())) {
            value = normalizeScalar(filter.getValue());
        } else {
            List<String> values = new ArrayList<>();
            for (Object v : enumerateScalars(filter.getValue())) {
                if (isScalar(v)) {
                    values.add(normalizeScalar(v));
                }
            }
            value = Collections.unmodifiableList(values);
        }
        String type = filter.getType() == null || filter.getType().isEmpty() ? null : filter.getType();
        return new Filter(filter.getField(), filter.getOperator(), value, type);
    }

    private static List<Object> enumerateScalars(Object value) {
        if (value == null) {
            return Collections.singletonList(null);
        }
        if (value instanceof String) {
            return List.of(value);
        }
        List<Object> items = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(items::add);
            return items;
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[] objects) {
                items.addAll(Arrays.asList(objects));
            } else {
                for (int i = 0; i < Array.getLength(value); i++) {
                    items.add(Array.get(value, i));
                }
            }
            return items;
        }
        return List.of(value);
    }

    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Boolean || value instanceof JsonNumber || value instanceof Number;
    }

    private static String normalizeScalar(Object value) {
        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof JsonNumber n) {
            return n.literal();
        }
        String formatted = QueryParams.format(value);
        return formatted == null ? "" : formatted;
    }

    private static boolean isBlank(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean white = (c >= 0x09 && c <= 0x0D) || c == 0x85 || Character.isSpaceChar(c);
            if (!white) {
                return false;
            }
        }
        return true;
    }

    // -- Jackson ---------------------------------------------------------------------------

    /** Serialises a {@link FilterSet} as its wire format when embedded in a larger body. */
    static final class WireSerializer extends StdSerializer<FilterSet> {
        private static final long serialVersionUID = 1L;

        WireSerializer() {
            super(FilterSet.class);
        }

        @Override
        public void serialize(FilterSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeTree(value.toJsonNode());
        }
    }

    /** Reads a {@link FilterSet} from a bare list of groups or the {@code SearchRequest} envelope. */
    static final class WireDeserializer extends StdDeserializer<FilterSet> {
        private static final long serialVersionUID = 1L;

        WireDeserializer() {
            super(FilterSet.class);
        }

        @Override
        public FilterSet deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return ingest(readTree(parser));
        }
    }
}
