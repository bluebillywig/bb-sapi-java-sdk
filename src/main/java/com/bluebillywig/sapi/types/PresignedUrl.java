package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A presigned S3 URL for one upload part. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class PresignedUrl {
    /** The presigned PUT URL. */
    @JsonProperty("presignedUrl")
    private String presignedUrlValue = "";

    /** Byte offset of this part in the file (defaults to 0). */
    @JsonProperty("offset")
    private Long offset;

    /** Size of this part in bytes (defaults to the whole file). */
    @JsonProperty("chunkSize")
    private Long chunkSize;

    /** @return the presigned PUT URL */
    public String getPresignedUrlValue() {
        return presignedUrlValue;
    }

    /**
     * @param presignedUrlValue the presigned PUT URL
     * @return this
     */
    public PresignedUrl setPresignedUrlValue(String presignedUrlValue) {
        this.presignedUrlValue = presignedUrlValue;
        return this;
    }

    /** @return byte offset of this part in the file (defaults to 0) */
    public Long getOffset() {
        return offset;
    }

    /**
     * @param offset byte offset of this part in the file (defaults to 0)
     * @return this
     */
    public PresignedUrl setOffset(Long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * @param offset byte offset of this part in the file (defaults to 0)
     * @return this
     */
    public PresignedUrl setOffset(long offset) {
        this.offset = offset;
        return this;
    }

    /** @return size of this part in bytes (defaults to the whole file) */
    public Long getChunkSize() {
        return chunkSize;
    }

    /**
     * @param chunkSize size of this part in bytes (defaults to the whole file)
     * @return this
     */
    public PresignedUrl setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * @param chunkSize size of this part in bytes (defaults to the whole file)
     * @return this
     */
    public PresignedUrl setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }
}
