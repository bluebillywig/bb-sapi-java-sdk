package com.bluebillywig.sapi.contracts;

import com.bluebillywig.sapi.SapiResponse;

/**
 * An entity that can be updated by ID.
 *
 * @param <P> the property bag type sent as the JSON body
 */
public interface Updatable<P> {
    /**
     * Updates an existing entity.
     *
     * @param id the entity ID
     * @param props the properties
     * @return the response
     */
    SapiResponse update(String id, P props);

    /**
     * Updates an existing entity by numeric ID.
     *
     * @param id the entity ID
     * @param props the properties
     * @return the response
     */
    default SapiResponse update(long id, P props) {
        return update(Long.toString(id), props);
    }
}
