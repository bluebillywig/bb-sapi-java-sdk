package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writable subtitle properties. Unset (null) properties are omitted from the request. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class SubtitleProps {
    /** Status. */
    @JsonProperty("status")
    private String status;

    /** Media clip ID. */
    @JsonProperty("mediaclipId")
    private Long mediaclipId;

    /** Language ID. */
    @JsonProperty("languageId")
    private Long languageId;

    /** ISO code. */
    @JsonProperty("isocode")
    private String isocode;

    /** Original file name. */
    @JsonProperty("originalfilename")
    private String originalfilename;

    /** Upload identifier. */
    @JsonProperty("uploadIdentifier")
    private String uploadIdentifier;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return status */
    public String getStatus() {
        return status;
    }

    /** @param status status @return this */
    public SubtitleProps setStatus(String status) {
        this.status = status;
        return this;
    }

    /** @return media clip ID */
    public Long getMediaclipId() {
        return mediaclipId;
    }

    /** @param mediaclipId media clip ID @return this */
    public SubtitleProps setMediaclipId(Long mediaclipId) {
        this.mediaclipId = mediaclipId;
        return this;
    }

    /** @param mediaclipId media clip ID @return this */
    public SubtitleProps setMediaclipId(long mediaclipId) {
        this.mediaclipId = mediaclipId;
        return this;
    }

    /** @return language ID */
    public Long getLanguageId() {
        return languageId;
    }

    /** @param languageId language ID @return this */
    public SubtitleProps setLanguageId(Long languageId) {
        this.languageId = languageId;
        return this;
    }

    /** @param languageId language ID @return this */
    public SubtitleProps setLanguageId(long languageId) {
        this.languageId = languageId;
        return this;
    }

    /** @return iSO code */
    public String getIsocode() {
        return isocode;
    }

    /** @param isocode iSO code @return this */
    public SubtitleProps setIsocode(String isocode) {
        this.isocode = isocode;
        return this;
    }

    /** @return original file name */
    public String getOriginalfilename() {
        return originalfilename;
    }

    /** @param originalfilename original file name @return this */
    public SubtitleProps setOriginalfilename(String originalfilename) {
        this.originalfilename = originalfilename;
        return this;
    }

    /** @return upload identifier */
    public String getUploadIdentifier() {
        return uploadIdentifier;
    }

    /** @param uploadIdentifier upload identifier @return this */
    public SubtitleProps setUploadIdentifier(String uploadIdentifier) {
        this.uploadIdentifier = uploadIdentifier;
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
    public SubtitleProps setAdditionalProperties(Map<String, Object> additionalProperties) {
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
