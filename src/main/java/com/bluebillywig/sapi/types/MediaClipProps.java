package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writable media clip properties. Unset (null) properties are omitted from the request. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class MediaClipProps {
    /** Clip title. */
    @JsonProperty("title")
    private String title;

    /** Clip description. */
    @JsonProperty("description")
    private String description;

    /** Author. */
    @JsonProperty("author")
    private String author;

    /** Copyright notice. */
    @JsonProperty("copyright")
    private String copyright;

    /** Deeplink. */
    @JsonProperty("deeplink")
    private String deeplink;

    /** Status, e.g. {@code published} or {@code draft}. */
    @JsonProperty("status")
    private String status;

    /** Use type, e.g. {@code editorial} or {@code commercial}. */
    @JsonProperty("usetype")
    private String usetype;

    /** Fit mode. */
    @JsonProperty("fitmode")
    private String fitmode;

    /** Source type. */
    @JsonProperty("sourcetype")
    private String sourcetype;

    /** Original file name. */
    @JsonProperty("originalfilename")
    private String originalfilename;

    /** Length. */
    @JsonProperty("length")
    private Long length;

    /** Category. */
    @JsonProperty("cat")
    private String cat;

    /** Playout override. */
    @JsonProperty("playoutoverride")
    private String playoutoverride;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return clip title */
    public String getTitle() {
        return title;
    }

    /**
     * @param title clip title
     * @return this
     */
    public MediaClipProps setTitle(String title) {
        this.title = title;
        return this;
    }

    /** @return clip description */
    public String getDescription() {
        return description;
    }

    /**
     * @param description clip description
     * @return this
     */
    public MediaClipProps setDescription(String description) {
        this.description = description;
        return this;
    }

    /** @return author */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author author
     * @return this
     */
    public MediaClipProps setAuthor(String author) {
        this.author = author;
        return this;
    }

    /** @return copyright notice */
    public String getCopyright() {
        return copyright;
    }

    /**
     * @param copyright copyright notice
     * @return this
     */
    public MediaClipProps setCopyright(String copyright) {
        this.copyright = copyright;
        return this;
    }

    /** @return deeplink */
    public String getDeeplink() {
        return deeplink;
    }

    /**
     * @param deeplink deeplink
     * @return this
     */
    public MediaClipProps setDeeplink(String deeplink) {
        this.deeplink = deeplink;
        return this;
    }

    /** @return status, e.g. {@code published} or {@code draft} */
    public String getStatus() {
        return status;
    }

    /**
     * @param status status, e.g. {@code published} or {@code draft}
     * @return this
     */
    public MediaClipProps setStatus(String status) {
        this.status = status;
        return this;
    }

    /** @return use type, e.g. {@code editorial} or {@code commercial} */
    public String getUsetype() {
        return usetype;
    }

    /**
     * @param usetype use type, e.g. {@code editorial} or {@code commercial}
     * @return this
     */
    public MediaClipProps setUsetype(String usetype) {
        this.usetype = usetype;
        return this;
    }

    /** @return fit mode */
    public String getFitmode() {
        return fitmode;
    }

    /**
     * @param fitmode fit mode
     * @return this
     */
    public MediaClipProps setFitmode(String fitmode) {
        this.fitmode = fitmode;
        return this;
    }

    /** @return source type */
    public String getSourcetype() {
        return sourcetype;
    }

    /**
     * @param sourcetype source type
     * @return this
     */
    public MediaClipProps setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
        return this;
    }

    /** @return original file name */
    public String getOriginalfilename() {
        return originalfilename;
    }

    /**
     * @param originalfilename original file name
     * @return this
     */
    public MediaClipProps setOriginalfilename(String originalfilename) {
        this.originalfilename = originalfilename;
        return this;
    }

    /** @return length */
    public Long getLength() {
        return length;
    }

    /**
     * @param length length
     * @return this
     */
    public MediaClipProps setLength(Long length) {
        this.length = length;
        return this;
    }

    /**
     * @param length length
     * @return this
     */
    public MediaClipProps setLength(long length) {
        this.length = length;
        return this;
    }

    /** @return category */
    public String getCat() {
        return cat;
    }

    /**
     * @param cat category
     * @return this
     */
    public MediaClipProps setCat(String cat) {
        this.cat = cat;
        return this;
    }

    /** @return playout override */
    public String getPlayoutoverride() {
        return playoutoverride;
    }

    /**
     * @param playoutoverride playout override
     * @return this
     */
    public MediaClipProps setPlayoutoverride(String playoutoverride) {
        this.playoutoverride = playoutoverride;
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
    public MediaClipProps setAdditionalProperties(Map<String, Object> additionalProperties) {
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
