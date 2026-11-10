package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writable playlist properties. Unset (null) properties are omitted from the request. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class PlaylistProps {
    /** Title. */
    @JsonProperty("title")
    private String title;

    /** Description. */
    @JsonProperty("description")
    private String description;

    /** Status. */
    @JsonProperty("status")
    private String status;

    /** Playlist type. */
    @JsonProperty("type")
    private String type;

    /** Query. */
    @JsonProperty("query")
    private String query;

    /** Media type. */
    @JsonProperty("mediatype")
    private String mediatype;

    /** Use type. */
    @JsonProperty("usetype")
    private String usetype;

    /** Copyright. */
    @JsonProperty("copyright")
    private String copyright;

    /** Author. */
    @JsonProperty("author")
    private String author;

    /** Deeplink URL. */
    @JsonProperty("deeplinkUrl")
    private String deeplinkUrl;

    /** Short title. */
    @JsonProperty("shortTitle")
    private String shortTitle;

    /** External URL. */
    @JsonProperty("externalUrl")
    private String externalUrl;

    /** Shuffle order. */
    @JsonProperty("shuffleOrder")
    private Boolean shuffleOrder;

    /** Use suggest. */
    @JsonProperty("useSuggest")
    private Boolean useSuggest;

    /** Extra suggest query. */
    @JsonProperty("extraSuggestQuery")
    private String extraSuggestQuery;

    /** Allow datasource. */
    @JsonProperty("allowDatasource")
    private Boolean allowDatasource;

    /** Limit. */
    @JsonProperty("limit")
    private Integer limit;

    /** Sort. */
    @JsonProperty("sort")
    private String sort;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return title */
    public String getTitle() {
        return title;
    }

    /** @param title title @return this */
    public PlaylistProps setTitle(String title) {
        this.title = title;
        return this;
    }

    /** @return description */
    public String getDescription() {
        return description;
    }

    /** @param description description @return this */
    public PlaylistProps setDescription(String description) {
        this.description = description;
        return this;
    }

    /** @return status */
    public String getStatus() {
        return status;
    }

    /** @param status status @return this */
    public PlaylistProps setStatus(String status) {
        this.status = status;
        return this;
    }

    /** @return playlist type */
    public String getType() {
        return type;
    }

    /** @param type playlist type @return this */
    public PlaylistProps setType(String type) {
        this.type = type;
        return this;
    }

    /** @return query */
    public String getQuery() {
        return query;
    }

    /** @param query query @return this */
    public PlaylistProps setQuery(String query) {
        this.query = query;
        return this;
    }

    /** @return media type */
    public String getMediatype() {
        return mediatype;
    }

    /** @param mediatype media type @return this */
    public PlaylistProps setMediatype(String mediatype) {
        this.mediatype = mediatype;
        return this;
    }

    /** @return use type */
    public String getUsetype() {
        return usetype;
    }

    /** @param usetype use type @return this */
    public PlaylistProps setUsetype(String usetype) {
        this.usetype = usetype;
        return this;
    }

    /** @return copyright */
    public String getCopyright() {
        return copyright;
    }

    /** @param copyright copyright @return this */
    public PlaylistProps setCopyright(String copyright) {
        this.copyright = copyright;
        return this;
    }

    /** @return author */
    public String getAuthor() {
        return author;
    }

    /** @param author author @return this */
    public PlaylistProps setAuthor(String author) {
        this.author = author;
        return this;
    }

    /** @return deeplink URL */
    public String getDeeplinkUrl() {
        return deeplinkUrl;
    }

    /** @param deeplinkUrl deeplink URL @return this */
    public PlaylistProps setDeeplinkUrl(String deeplinkUrl) {
        this.deeplinkUrl = deeplinkUrl;
        return this;
    }

    /** @return short title */
    public String getShortTitle() {
        return shortTitle;
    }

    /** @param shortTitle short title @return this */
    public PlaylistProps setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
        return this;
    }

    /** @return external URL */
    public String getExternalUrl() {
        return externalUrl;
    }

    /** @param externalUrl external URL @return this */
    public PlaylistProps setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }

    /** @return shuffle order */
    public Boolean getShuffleOrder() {
        return shuffleOrder;
    }

    /** @param shuffleOrder shuffle order @return this */
    public PlaylistProps setShuffleOrder(Boolean shuffleOrder) {
        this.shuffleOrder = shuffleOrder;
        return this;
    }

    /** @return use suggest */
    public Boolean getUseSuggest() {
        return useSuggest;
    }

    /** @param useSuggest use suggest @return this */
    public PlaylistProps setUseSuggest(Boolean useSuggest) {
        this.useSuggest = useSuggest;
        return this;
    }

    /** @return extra suggest query */
    public String getExtraSuggestQuery() {
        return extraSuggestQuery;
    }

    /** @param extraSuggestQuery extra suggest query @return this */
    public PlaylistProps setExtraSuggestQuery(String extraSuggestQuery) {
        this.extraSuggestQuery = extraSuggestQuery;
        return this;
    }

    /** @return allow datasource */
    public Boolean getAllowDatasource() {
        return allowDatasource;
    }

    /** @param allowDatasource allow datasource @return this */
    public PlaylistProps setAllowDatasource(Boolean allowDatasource) {
        this.allowDatasource = allowDatasource;
        return this;
    }

    /** @return limit */
    public Integer getLimit() {
        return limit;
    }

    /** @param limit limit @return this */
    public PlaylistProps setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /** @return sort */
    public String getSort() {
        return sort;
    }

    /** @param sort sort @return this */
    public PlaylistProps setSort(String sort) {
        this.sort = sort;
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
    public PlaylistProps setAdditionalProperties(Map<String, Object> additionalProperties) {
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
