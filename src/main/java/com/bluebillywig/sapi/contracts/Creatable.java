package com.bluebillywig.sapi.contracts;

import com.bluebillywig.sapi.SapiResponse;

/**
 * An entity that can be created.
 *
 * @param <P> the property bag type sent as the JSON body
 */
public interface Creatable<P> {
    /**
     * Creates a new entity.
     *
     * @param props the properties
     * @return the response
     */
    SapiResponse create(P props);
}
