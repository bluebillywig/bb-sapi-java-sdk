package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Channel detail page configuration. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ChannelDetailPageConfig {
    /** Player alignment. */
    @JsonProperty("playerAlignment")
    private String playerAlignment;

    /** Within border. */
    @JsonProperty("withinBorder")
    private Boolean withinBorder;

    /** Background color. */
    @JsonProperty("backgroundColor")
    private String backgroundColor;

    /** Show thumbnail as background. */
    @JsonProperty("showThumbnailAsBackground")
    private Boolean showThumbnailAsBackground;

    /** Enable background blur. */
    @JsonProperty("enableBackgroundBlur")
    private Boolean enableBackgroundBlur;

    /** Show related items. */
    @JsonProperty("showRelatedItems")
    private Boolean showRelatedItems;

    /** Related items layout. */
    @JsonProperty("relatedItemsLayout")
    private String relatedItemsLayout;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return player alignment */
    public String getPlayerAlignment() {
        return playerAlignment;
    }

    /**
     * @param playerAlignment player alignment
     * @return this
     */
    public ChannelDetailPageConfig setPlayerAlignment(String playerAlignment) {
        this.playerAlignment = playerAlignment;
        return this;
    }

    /** @return within border */
    public Boolean getWithinBorder() {
        return withinBorder;
    }

    /**
     * @param withinBorder within border
     * @return this
     */
    public ChannelDetailPageConfig setWithinBorder(Boolean withinBorder) {
        this.withinBorder = withinBorder;
        return this;
    }

    /** @return background color */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor background color
     * @return this
     */
    public ChannelDetailPageConfig setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /** @return show thumbnail as background */
    public Boolean getShowThumbnailAsBackground() {
        return showThumbnailAsBackground;
    }

    /**
     * @param showThumbnailAsBackground show thumbnail as background
     * @return this
     */
    public ChannelDetailPageConfig setShowThumbnailAsBackground(Boolean showThumbnailAsBackground) {
        this.showThumbnailAsBackground = showThumbnailAsBackground;
        return this;
    }

    /** @return enable background blur */
    public Boolean getEnableBackgroundBlur() {
        return enableBackgroundBlur;
    }

    /**
     * @param enableBackgroundBlur enable background blur
     * @return this
     */
    public ChannelDetailPageConfig setEnableBackgroundBlur(Boolean enableBackgroundBlur) {
        this.enableBackgroundBlur = enableBackgroundBlur;
        return this;
    }

    /** @return show related items */
    public Boolean getShowRelatedItems() {
        return showRelatedItems;
    }

    /**
     * @param showRelatedItems show related items
     * @return this
     */
    public ChannelDetailPageConfig setShowRelatedItems(Boolean showRelatedItems) {
        this.showRelatedItems = showRelatedItems;
        return this;
    }

    /** @return related items layout */
    public String getRelatedItemsLayout() {
        return relatedItemsLayout;
    }

    /**
     * @param relatedItemsLayout related items layout
     * @return this
     */
    public ChannelDetailPageConfig setRelatedItemsLayout(String relatedItemsLayout) {
        this.relatedItemsLayout = relatedItemsLayout;
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
    public ChannelDetailPageConfig setAdditionalProperties(Map<String, Object> additionalProperties) {
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
