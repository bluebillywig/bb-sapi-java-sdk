package com.bluebillywig.sapi.contracts;

import com.bluebillywig.sapi.SapiResponse;

/** An entity that can be deleted by ID. */
public interface Deletable {
    /**
     * Deletes an entity.
     *
     * @param id the entity ID
     * @return the response
     */
    SapiResponse delete(String id);

    /**
     * Deletes an entity by numeric ID.
     *
     * @param id the entity ID
     * @return the response
     */
    default SapiResponse delete(long id) {
        return delete(Long.toString(id));
    }
}
