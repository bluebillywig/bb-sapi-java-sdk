package com.bluebillywig.sapi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A register that lazily instantiates entities by name and caches them. This is the Java
 * counterpart of the Node SDK's Proxy-based entity register.
 */
public final class EntityRegister {
    private final Sdk sdk;
    private final Map<String, Function<Sdk, Entity>> factories = new LinkedHashMap<>();
    private final Map<String, Entity> cache = new HashMap<>();

    /**
     * Creates a register for the given SDK and registrations.
     *
     * @param sdk the SDK
     * @param registrations the entity registrations
     */
    public EntityRegister(Sdk sdk, Iterable<EntityRegistration> registrations) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
        for (EntityRegistration registration : registrations) {
            factories.put(registration.name(), registration.factory());
        }
    }

    /** @return the registered entity names */
    public Collection<String> names() {
        return Collections.unmodifiableCollection(factories.keySet());
    }

    /**
     * Whether an entity with this name is registered.
     *
     * @param name the entity name
     * @return whether it is registered
     */
    public boolean contains(String name) {
        return factories.containsKey(name);
    }

    /**
     * Returns the (cached) entity for a name.
     *
     * @param name the entity name
     * @return the entity, or {@code null} when unknown
     */
    public synchronized Entity get(String name) {
        Entity cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        Function<Sdk, Entity> factory = factories.get(name);
        if (factory == null) {
            return null;
        }
        Entity instance = factory.apply(sdk);
        cache.put(name, instance);
        return instance;
    }

    /**
     * Returns the (cached) entity for a name as {@code type}.
     *
     * @param name the entity name
     * @param type the expected entity class
     * @param <T> the expected entity type
     * @return the entity, or {@code null} when unknown or of another type
     */
    public <T extends Entity> T get(String name, Class<T> type) {
        Entity entity = get(name);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }
}
