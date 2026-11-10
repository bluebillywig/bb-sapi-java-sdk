package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writable channel properties. Unset (null) properties are omitted from the request. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ChannelProps {
    /** Configuration. */
    @JsonProperty("config")
    private ChannelConfig config;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return configuration */
    public ChannelConfig getConfig() {
        return config;
    }

    /** @param config configuration @return this */
    public ChannelProps setConfig(ChannelConfig config) {
        this.config = config;
        return this;
    }

    /**
     * The additional properties, created on first access. Entries are written to and read from
     * the JSON object verbatim, next to the modelled properties.
     *
     * @return the (mutable) additional properties, never {@code null}
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        if (additionalProperties == null) {
            additionalProperties = new LinkedHashMap<>();
        }
        return additionalProperties;
    }

    /**
     * Replaces the additional properties.
     *
     * @param additionalProperties the properties to send verbatim, or {@code null} for none
     * @return this
     */
    public ChannelProps setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    /**
     * Adds one additional property (also the Jackson hook for unmodelled properties).
     *
     * @param name the property name
     * @param value the property value
     */
    @JsonAnySetter
    public void addAdditionalProperty(String name, Object value) {
        getAdditionalProperties().put(name, value);
    }
}
