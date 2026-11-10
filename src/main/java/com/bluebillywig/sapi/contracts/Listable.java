package com.bluebillywig.sapi.contracts;

import com.bluebillywig.sapi.SapiResponse;

/** An entity that can be listed (paged and sorted). */
public interface Listable {
    /**
     * Lists entities.
     *
     * @param limit the page size
     * @param offset the page offset
     * @param sort the sort expression
     * @return the response
     */
    SapiResponse list(int limit, int offset, String sort);

    /**
     * Lists entities with the default paging: limit 15, offset 0, {@code createddate desc}.
     *
     * @return the response
     */
    default SapiResponse list() {
        return list(15, 0, "createddate desc");
    }

    /**
     * Lists entities with the default offset and sort.
     *
     * @param limit the page size
     * @return the response
     */
    default SapiResponse list(int limit) {
        return list(limit, 0, "createddate desc");
    }

    /**
     * Lists entities with the default sort.
     *
     * @param limit the page size
     * @param offset the page offset
     * @return the response
     */
    default SapiResponse list(int limit, int offset) {
        return list(limit, offset, "createddate desc");
    }
}
