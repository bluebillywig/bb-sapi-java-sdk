package com.bluebillywig.sapi.contracts;

import com.bluebillywig.sapi.SapiResponse;

/** An entity that can be fetched by ID. */
public interface Gettable {
    /**
     * Gets a single entity by ID.
     *
     * @param id the entity ID
     * @return the response
     */
    SapiResponse get(String id);

    /**
     * Gets a single entity by numeric ID.
     *
     * @param id the entity ID
     * @return the response
     */
    default SapiResponse get(long id) {
        return get(Long.toString(id));
    }
}
