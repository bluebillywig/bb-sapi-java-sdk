package com.bluebillywig.sapi;

import java.util.Objects;

/** Abstract base class for all SAPI entities. Provides access to the SDK instance. */
public abstract class Entity {
    private final Sdk sdk;

    /**
     * Creates an entity bound to an SDK instance.
     *
     * @param sdk the SDK
     */
    protected Entity(Sdk sdk) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
    }

    /** @return the SDK instance this entity belongs to */
    public Sdk getSdk() {
        return sdk;
    }
}
