package com.bluebillywig.sapi.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Operators SAPI understands in a filterset. */
public enum FilterOperator {
    /** {@code is}. */
    IS("is"),
    /** {@code isNot}. */
    IS_NOT("isNot"),
    /** {@code isAnyOf}. */
    IS_ANY_OF("isAnyOf"),
    /** {@code isNotAnyOf}. */
    IS_NOT_ANY_OF("isNotAnyOf"),
    /** {@code isEmpty} (presence test; needs no value). */
    IS_EMPTY("isEmpty"),
    /** {@code isNotEmpty} (presence test; needs no value). */
    IS_NOT_EMPTY("isNotEmpty"),
    /** {@code contains}. */
    CONTAINS("contains"),
    /** {@code containsAnyOf}. */
    CONTAINS_ANY_OF("containsAnyOf"),
    /** {@code containsAllOf}. */
    CONTAINS_ALL_OF("containsAllOf"),
    /** {@code doesNotContain}. */
    DOES_NOT_CONTAIN("doesNotContain"),
    /** {@code doesNotContainAnyOf}. */
    DOES_NOT_CONTAIN_ANY_OF("doesNotContainAnyOf"),
    /** {@code isBefore}. */
    IS_BEFORE("isBefore"),
    /** {@code isAfter}. */
    IS_AFTER("isAfter"),
    /** {@code isSmallerThan}. */
    IS_SMALLER_THAN("isSmallerThan"),
    /** {@code isGreaterThan}. */
    IS_GREATER_THAN("isGreaterThan"),
    /** {@code isInTheLast}. */
    IS_IN_THE_LAST("isInTheLast"),
    /** {@code isNotInTheLast}. */
    IS_NOT_IN_THE_LAST("isNotInTheLast");

    private static final List<String> WIRE_NAMES = Arrays.stream(values()).map(FilterOperator::toWire).toList();

    private final String wire;

    FilterOperator(String wire) {
        this.wire = wire;
    }

    /** @return all wire names, in declaration order */
    public static Collection<String> wireNames() {
        return WIRE_NAMES;
    }

    /** @return the wire name of this operator, e.g. {@code isNot} */
    public String toWire() {
        return wire;
    }

    /**
     * Parses a wire name.
     *
     * @param wireName the wire name
     * @return the operator
     * @throws IllegalArgumentException for an unknown operator
     */
    public static FilterOperator parse(String wireName) {
        return tryParse(wireName).orElseThrow(() -> new IllegalArgumentException(
                "Unknown filter operator '" + (wireName == null ? "" : wireName) + "'. Known operators: "
                        + String.join(", ", WIRE_NAMES) + "."));
    }

    /**
     * Tries to parse a wire name.
     *
     * @param wireName the wire name, possibly {@code null}
     * @return the operator, or empty for an unknown name
     */
    public static Optional<FilterOperator> tryParse(String wireName) {
        if (wireName != null) {
            for (FilterOperator op : values()) {
                if (op.wire.equals(wireName)) {
                    return Optional.of(op);
                }
            }
        }
        return Optional.empty();
    }

    /** @return whether the operator tests presence, so it is meaningful without a value */
    public boolean isValueless() {
        return this == IS_EMPTY || this == IS_NOT_EMPTY;
    }
}
