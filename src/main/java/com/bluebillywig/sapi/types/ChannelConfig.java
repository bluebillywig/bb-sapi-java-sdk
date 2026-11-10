package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Channel configuration. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ChannelConfig {
    /** Play-in mode. */
    @JsonProperty("playIn")
    private String playIn;

    /** Detail page configuration. */
    @JsonProperty("detailPageConfig")
    private ChannelDetailPageConfig detailPageConfig;

    /** Blocks. */
    @JsonProperty("blocks")
    private List<JsonNode> blocks;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return play-in mode */
    public String getPlayIn() {
        return playIn;
    }

    /** @param playIn play-in mode @return this */
    public ChannelConfig setPlayIn(String playIn) {
        this.playIn = playIn;
        return this;
    }

    /** @return detail page configuration */
    public ChannelDetailPageConfig getDetailPageConfig() {
        return detailPageConfig;
    }

    /** @param detailPageConfig detail page configuration @return this */
    public ChannelConfig setDetailPageConfig(ChannelDetailPageConfig detailPageConfig) {
        this.detailPageConfig = detailPageConfig;
        return this;
    }

    /** @return blocks */
    public List<JsonNode> getBlocks() {
        return blocks;
    }

    /** @param blocks blocks @return this */
    public ChannelConfig setBlocks(List<JsonNode> blocks) {
        this.blocks = blocks;
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
    public ChannelConfig setAdditionalProperties(Map<String, Object> additionalProperties) {
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
