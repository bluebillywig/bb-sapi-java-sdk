package com.bluebillywig.sapi.entities;

import com.bluebillywig.sapi.Entity;
import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.util.QueryParams;
import java.util.Objects;

/** Thumbnail and poster URL helpers. */
public final class Thumbnail extends Entity {
    /**
     * Creates the entity.
     *
     * @param sdk the SDK
     */
    public Thumbnail(Sdk sdk) {
        super(sdk);
    }

    /**
     * Constructs an absolute thumbnail URL for the original image size.
     *
     * @param relativeImagePath the relative path to the image (with or without leading slash)
     * @return the absolute URL
     */
    public String getAbsoluteImagePath(String relativeImagePath) {
        return getAbsoluteImagePath(relativeImagePath, 0, 0);
    }

    /**
     * Constructs an absolute thumbnail URL with optional dimensions.
     *
     * @param relativeImagePath the relative path to the image (with or without leading slash)
     * @param width desired width in pixels (0 for original); must be non-negative
     * @param height desired height in pixels (0 for original); must be non-negative
     * @return the absolute URL
     * @throws IllegalArgumentException if width or height is negative
     */
    public String getAbsoluteImagePath(String relativeImagePath, int width, int height) {
        Objects.requireNonNull(relativeImagePath, "relativeImagePath");
        if (width < 0) {
            throw new IllegalArgumentException("Given width is lower than 0.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("Given height is lower than 0.");
        }

        int start = 0;
        while (start < relativeImagePath.length() && relativeImagePath.charAt(start) == '/') {
            start++;
        }
        return getSdk().getBaseUri() + "/image/" + width + "/" + height + "/" + relativeImagePath.substring(start);
    }

    /**
     * Absolute URL of a media clip's poster image, with the service choosing the dimensions.
     *
     * @param mediaClipId the media clip ID
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(String mediaClipId) {
        return getMediaClipPosterPath(mediaClipId, null, null, null);
    }

    /**
     * Absolute URL of a media clip's poster image.
     *
     * @param mediaClipId the media clip ID
     * @param width the width, or {@code null}
     * @param height the height, or {@code null}
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(String mediaClipId, Integer width, Integer height) {
        return getMediaClipPosterPath(mediaClipId, width, height, null);
    }

    /**
     * Absolute URL of a media clip's poster image.
     *
     * <p>Use this rather than building a URL from the clip payload. A clip's {@code src} is its
     * SOURCE MEDIA file, so {@code defaultMediaAssetPath + clip.src} yields a link to a .mov, and
     * the service says as much, replying "Invalid src mime type: video/quicktime". That mistake
     * shows up as a grid full of broken images.
     *
     * <p>A {@code null} dimension (or one outside 0..99999) lets the service choose
     * ({@code default}).
     *
     * <p>A draft (unpublished) clip's poster is not public. Pass an RPC token minted from the
     * READ-ONLY key, never the write key, because this URL ends up in page source, to see
     * those. Note the token is time-based (HOTP, 120 s window by default), so the URL is
     * short-lived.
     *
     * @param mediaClipId the media clip ID
     * @param width the width, or {@code null}
     * @param height the height, or {@code null}
     * @param rpcToken an RPC token for draft clips, or {@code null}
     * @return the poster URL
     */
    public String getMediaClipPosterPath(String mediaClipId, Integer width, Integer height, String rpcToken) {
        Objects.requireNonNull(mediaClipId, "mediaClipId");

        String url = getSdk().getBaseUri() + "/mediaclip/" + QueryParams.encode(mediaClipId)
                + "/spthumbnail/" + dimension(width) + "/" + dimension(height) + ".webp";

        return rpcToken == null || rpcToken.isEmpty()
                ? url
                : url + "?useSession=true&rpctoken=" + QueryParams.encode(rpcToken);
    }

    /**
     * Absolute URL of a media clip's poster image, with the service choosing the dimensions.
     *
     * @param mediaClipId the media clip ID
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(long mediaClipId) {
        return getMediaClipPosterPath(Long.toString(mediaClipId));
    }

    /**
     * Absolute URL of a media clip's poster image.
     *
     * @param mediaClipId the media clip ID
     * @param width the width, or {@code null}
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(long mediaClipId, Integer width) {
        return getMediaClipPosterPath(Long.toString(mediaClipId), width, null, null);
    }

    /**
     * Absolute URL of a media clip's poster image.
     *
     * @param mediaClipId the media clip ID
     * @param width the width, or {@code null}
     * @param height the height, or {@code null}
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(long mediaClipId, Integer width, Integer height) {
        return getMediaClipPosterPath(Long.toString(mediaClipId), width, height, null);
    }

    /**
     * Absolute URL of a media clip's poster image.
     *
     * @param mediaClipId the media clip ID
     * @param width the width, or {@code null}
     * @param height the height, or {@code null}
     * @param rpcToken an RPC token for draft clips, or {@code null}
     * @return the poster URL
     * @see #getMediaClipPosterPath(String, Integer, Integer, String)
     */
    public String getMediaClipPosterPath(long mediaClipId, Integer width, Integer height, String rpcToken) {
        return getMediaClipPosterPath(Long.toString(mediaClipId), width, height, rpcToken);
    }

    private static String dimension(Integer value) {
        return value != null && value >= 0 && value < 100000 ? Integer.toString(value) : "default";
    }
}
