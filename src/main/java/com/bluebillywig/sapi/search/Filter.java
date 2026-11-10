package com.bluebillywig.sapi.search;

import java.util.List;
import java.util.Objects;

/**
 * One condition in a filterset. The value may be a scalar (string, number, boolean) or a
 * sequence of scalars (an {@link Iterable} or an array); numbers and booleans are normalised to
 * strings on the wire by {@link FilterSet#toArray()}: the backend's compiler mangles a JSON
 * {@code true} into {@code "1"} (which matches nothing, silently) and its empty-value guard drops
 * {@code false} outright, while numbers work but only ever appear as strings in what OVP6 sends.
 */
public final class Filter {
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    private final String type;

    /**
     * Creates a filter without a value (for presence operators).
     *
     * @param field the field to filter on
     * @param operator the operator
     */
    public Filter(String field, FilterOperator operator) {
        this(field, operator, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param field the field to filter on
     * @param operator the operator
     * @param value a string, number, boolean, a sequence of those, or {@code null}
     */
    public Filter(String field, FilterOperator operator, Object value) {
        this(field, operator, value, null);
    }

    /**
     * Creates a filter.
     *
     * @param field the field to filter on
     * @param operator the operator
     * @param value a string, number, boolean, a sequence of those, or {@code null}
     * @param type constrain to an entity type (mediaclip, project, search), or {@code null}
     */
    public Filter(String field, FilterOperator operator, Object value, String type) {
        this.field = Objects.requireNonNull(field, "field");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.value = value;
        this.type = type;
    }

    /** @return the field to filter on */
    public String getField() {
        return field;
    }

    /** @return the operator */
    public FilterOperator getOperator() {
        return operator;
    }

    /** @return the value: a scalar, a sequence of scalars, or {@code null} */
    public Object getValue() {
        return value;
    }

    /** @return the entity type constraint (mediaclip, project, search), or {@code null} */
    public String getType() {
        return type;
    }

    /** @return the value as a single string when it is a string; otherwise {@code null} */
    public String getScalarValue() {
        return value instanceof String s ? s : null;
    }

    /** @return the value as a list of strings when it is one; otherwise {@code null} */
    public List<String> getListValue() {
        if (value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            return list.stream().map(String.class::cast).toList();
        }
        return null;
    }
}
