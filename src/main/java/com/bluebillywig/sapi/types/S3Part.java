package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/** An uploaded S3 part, as sent to {@code completeUpload}. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class S3Part {
    /** The part's ETag (without quotes). */
    @JsonProperty("ETag")
    private String eTag = "";

    /** The part number. */
    @JsonProperty("PartNumber")
    private String partNumber = "";

    /** Creates an empty part. */
    public S3Part() {
    }

    /**
     * Creates a part.
     *
     * @param eTag the part's ETag (without quotes)
     * @param partNumber the part number
     */
    public S3Part(String eTag, String partNumber) {
        this.eTag = eTag;
        this.partNumber = partNumber;
    }

    /** @return the part's ETag (without quotes) */
    public String getETag() {
        return eTag;
    }

    /**
     * @param eTag the part's ETag (without quotes)
     * @return this
     */
    public S3Part setETag(String eTag) {
        this.eTag = eTag;
        return this;
    }

    /** @return the part number */
    public String getPartNumber() {
        return partNumber;
    }

    /**
     * @param partNumber the part number
     * @return this
     */
    public S3Part setPartNumber(String partNumber) {
        this.partNumber = partNumber;
        return this;
    }
}
