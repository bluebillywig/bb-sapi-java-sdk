package com.bluebillywig.sapi.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A group of filters. Groups are AND-ed with each other, filters within a group are OR-ed. */
public final class FilterGroup {
    private final List<Filter> filters;

    /**
     * Creates a group.
     *
     * @param filters the filters in the group
     */
    public FilterGroup(Iterable<Filter> filters) {
        Objects.requireNonNull(filters, "filters");
        List<Filter> copy = new ArrayList<>();
        filters.forEach(copy::add);
        this.filters = Collections.unmodifiableList(copy);
    }

    /**
     * Creates a group.
     *
     * @param filters the filters in the group
     */
    public FilterGroup(Filter... filters) {
        this(Arrays.asList(filters));
    }

    /** @return the filters in this group */
    public List<Filter> getFilters() {
        return filters;
    }
}
