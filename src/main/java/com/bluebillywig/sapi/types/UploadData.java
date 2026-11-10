package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Upload configuration as returned by {@code MediaClip.initializeUpload}. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class UploadData {
    /** Number of parts. */
    @JsonProperty("chunks")
    private Integer chunks;

    /** One presigned URL per part. */
    @JsonProperty("presignedUrls")
    private List<PresignedUrl> presignedUrls;

    /** S3 object key (multi-part only). */
    @JsonProperty("key")
    private String key;

    /** S3 multi-part upload ID (multi-part only). */
    @JsonProperty("uploadId")
    private String uploadId;

    /** S3 URL to list uploaded parts (for progress polling). */
    @JsonProperty("listPartsUrl")
    private String listPartsUrl;

    /** S3 URL to check whether the object exists (upload complete). */
    @JsonProperty("headObjectUrl")
    private String headObjectUrl;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return number of parts */
    public Integer getChunks() {
        return chunks;
    }

    /** @param chunks number of parts @return this */
    public UploadData setChunks(Integer chunks) {
        this.chunks = chunks;
        return this;
    }

    /** @return one presigned URL per part */
    public List<PresignedUrl> getPresignedUrls() {
        return presignedUrls;
    }

    /** @param presignedUrls one presigned URL per part @return this */
    public UploadData setPresignedUrls(List<PresignedUrl> presignedUrls) {
        this.presignedUrls = presignedUrls;
        return this;
    }

    /** @return s3 object key (multi-part only) */
    public String getKey() {
        return key;
    }

    /** @param key s3 object key (multi-part only) @return this */
    public UploadData setKey(String key) {
        this.key = key;
        return this;
    }

    /** @return s3 multi-part upload ID (multi-part only) */
    public String getUploadId() {
        return uploadId;
    }

    /** @param uploadId s3 multi-part upload ID (multi-part only) @return this */
    public UploadData setUploadId(String uploadId) {
        this.uploadId = uploadId;
        return this;
    }

    /** @return s3 URL to list uploaded parts (for progress polling) */
    public String getListPartsUrl() {
        return listPartsUrl;
    }

    /** @param listPartsUrl s3 URL to list uploaded parts (for progress polling) @return this */
    public UploadData setListPartsUrl(String listPartsUrl) {
        this.listPartsUrl = listPartsUrl;
        return this;
    }

    /** @return s3 URL to check whether the object exists (upload complete) */
    public String getHeadObjectUrl() {
        return headObjectUrl;
    }

    /** @param headObjectUrl s3 URL to check whether the object exists (upload complete) @return this */
    public UploadData setHeadObjectUrl(String headObjectUrl) {
        this.headObjectUrl = headObjectUrl;
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
    public UploadData setAdditionalProperties(Map<String, Object> additionalProperties) {
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
