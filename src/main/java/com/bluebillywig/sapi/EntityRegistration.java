package com.bluebillywig.sapi;

import java.util.function.Function;

/**
 * A named entity factory registration.
 *
 * @param name the entity name, e.g. {@code mediaclip}
 * @param factory creates the entity for an SDK instance
 */
public record EntityRegistration(String name, Function<Sdk, Entity> factory) {
}
