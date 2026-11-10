package com.bluebillywig.sapi.entities;

import com.bluebillywig.sapi.Entity;
import com.bluebillywig.sapi.RequestOptions;
import com.bluebillywig.sapi.SapiResponse;
import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.contracts.Creatable;
import com.bluebillywig.sapi.contracts.Deletable;
import com.bluebillywig.sapi.contracts.Gettable;
import com.bluebillywig.sapi.contracts.Listable;
import com.bluebillywig.sapi.contracts.Updatable;
import com.bluebillywig.sapi.search.FilterSet;
import com.bluebillywig.sapi.types.MediaClipProps;
import com.bluebillywig.sapi.types.PresignedUrl;
import com.bluebillywig.sapi.types.S3Part;
import com.bluebillywig.sapi.types.UploadData;
import com.bluebillywig.sapi.util.QueryParams;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import static com.bluebillywig.sapi.util.QueryParams.param;

/** CRUD, search and upload operations on {@code /sapi/mediaclip}. */
public final class MediaClip extends Entity
        implements Listable, Gettable, Creatable<MediaClipProps>, Updatable<MediaClipProps>, Deletable {
    private static final String PATH = "/sapi/mediaclip";

    /** Default delay (in ms) between upload progress polling requests. */
    public static final int DEFAULT_UPLOAD_PROGRESS_POLL_INTERVAL = 2000;

    /** Maps file extensions to MIME types for media upload content-type detection. */
    private static final Map<String, String> MIME_MAP = Map.ofEntries(
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".ogg", "video/ogg"),
            Map.entry(".ogv", "video/ogg"),
            Map.entry(".avi", "video/x-msvideo"),
            Map.entry(".mov", "video/quicktime"),
            Map.entry(".wmv", "video/x-ms-wmv"),
            Map.entry(".flv", "video/x-flv"),
            Map.entry(".mkv", "video/x-matroska"),
            Map.entry(".m4v", "video/x-m4v"),
            Map.entry(".3gp", "video/3gpp"),
            Map.entry(".3g2", "video/3gpp2"),
            Map.entry(".ts", "video/mp2t"),
            Map.entry(".mts", "video/mp2t"),
            Map.entry(".m2ts", "video/mp2t"),
            Map.entry(".m3u8", "application/vnd.apple.mpegurl"),
            Map.entry(".mpd", "application/dash+xml"),
            Map.entry(".mp3", "audio/mpeg"),
            Map.entry(".wav", "audio/wav"),
            Map.entry(".aac", "audio/aac"),
            Map.entry(".m4a", "audio/mp4"),
            Map.entry(".flac", "audio/flac"),
            Map.entry(".wma", "audio/x-ms-wma"),
            Map.entry(".opus", "audio/opus"),
            Map.entry(".oga", "audio/ogg"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".bmp", "image/bmp"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".avif", "image/avif"),
            Map.entry(".heif", "image/heif"),
            Map.entry(".heic", "image/heic"),
            Map.entry(".mxf", "application/mxf"));

    /**
     * Creates the entity.
     *
     * @param sdk the SDK
     */
    public MediaClip(Sdk sdk) {
        super(sdk);
    }

    /**
     * Returns the MIME type for a file path by extension.
     *
     * @param filePath the file path
     * @return the MIME type, or {@code application/octet-stream}
     */
    public static String getMimeType(String filePath) {
        int separator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String name = filePath.substring(separator + 1);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 && dot < name.length() - 1 ? name.substring(dot) : "";
        return MIME_MAP.getOrDefault(ext.toLowerCase(Locale.ROOT), "application/octet-stream");
    }

    // -- search / list ---------------------------------------------------------------------

    /**
     * Search media clips with the default paging (limit 15, offset 0, sort
     * {@code createddate desc}, query {@code *}).
     *
     * @param filterSet groups are AND-ed, filters within a group OR-ed
     * @return the response
     * @see #search(FilterSet, int, int, String, String, Iterable)
     */
    public SapiResponse search(FilterSet filterSet) {
        return search(filterSet, 15, 0, "createddate desc", "*", null);
    }

    /**
     * Search media clips with the default offset, sort and query.
     *
     * @param filterSet groups are AND-ed, filters within a group OR-ed
     * @param limit page size
     * @return the response
     * @see #search(FilterSet, int, int, String, String, Iterable)
     */
    public SapiResponse search(FilterSet filterSet, int limit) {
        return search(filterSet, limit, 0, "createddate desc", "*", null);
    }

    /**
     * Search media clips without raw Solr filters.
     *
     * @param filterSet groups are AND-ed, filters within a group OR-ed
     * @param limit page size
     * @param offset page offset
     * @param sort sort expression
     * @param query free-text query; {@code *} for everything
     * @return the response
     * @see #search(FilterSet, int, int, String, String, Iterable)
     */
    public SapiResponse search(FilterSet filterSet, int limit, int offset, String sort, String query) {
        return search(filterSet, limit, offset, sort, query, null);
    }

    /**
     * Search media clips using a filterset: the filtered counterpart to {@link #list}, which can
     * only page and sort. The filterset goes over the wire as JSON and SAPI compiles it, exactly
     * as the OVP does. It is deliberately not compiled here.
     *
     * @param filterSet groups are AND-ed, filters within a group OR-ed
     * @param limit page size
     * @param offset page offset
     * @param sort sort expression
     * @param query free-text query; {@code *} for everything
     * @param filterQueries raw Solr filters, for the rare thing a filterset cannot express (may be
     *        {@code null}). These go out as indexed parameters ({@code fq[0]}, percent-encoded as
     *        {@code fq%5B0%5D} on the wire); SAPI ignores a repeated {@code fq=} and a nested
     *        {@code fq[][0]=}, in both cases without an error.
     * @return the response
     */
    public SapiResponse search(
            FilterSet filterSet, int limit, int offset, String sort, String query, Iterable<String> filterQueries) {
        Objects.requireNonNull(filterSet, "filterSet");

        Map<String, String> parameters = QueryParams.build(
                param("q", query), param("limit", limit), param("offset", offset), param("sort", sort));

        var wire = filterSet.toJsonNode();
        if (wire.size() > 0) {
            parameters.put("filterset", SapiJson.write(wire));
        }

        int index = 0;
        if (filterQueries != null) {
            for (String filterQuery : filterQueries) {
                parameters.put("fq[" + index++ + "]", filterQuery);
            }
        }

        return getSdk().sendRequest("GET", PATH, new RequestOptions().setQuery(parameters));
    }

    @Override
    public SapiResponse list(int limit, int offset, String sort) {
        return getSdk().sendRequest("GET", PATH, new RequestOptions().setQuery(QueryParams.build(
                param("limit", limit), param("offset", offset), param("sort", sort))));
    }

    // -- CRUD ------------------------------------------------------------------------------

    /**
     * Gets a media clip, including jobs.
     *
     * @param id the clip ID
     * @return the response
     */
    @Override
    public SapiResponse get(String id) {
        return get(id, null, true);
    }

    /**
     * Gets a media clip.
     *
     * @param id the clip ID
     * @param lang optional language, or {@code null}
     * @param includeJobs whether to include jobs (default {@code true})
     * @return the response
     */
    public SapiResponse get(String id, String lang, boolean includeJobs) {
        return getSdk().sendRequest("GET", PATH + "/" + QueryParams.encode(id), new RequestOptions()
                .setQuery(QueryParams.build(param("includejobs", includeJobs), param("lang", lang))));
    }

    /**
     * Gets a media clip.
     *
     * @param id the clip ID
     * @param lang optional language, or {@code null}
     * @param includeJobs whether to include jobs (default {@code true})
     * @return the response
     */
    public SapiResponse get(long id, String lang, boolean includeJobs) {
        return get(Long.toString(id), lang, includeJobs);
    }

    /**
     * Creates a media clip.
     *
     * @param props the properties
     * @return the response
     */
    @Override
    public SapiResponse create(MediaClipProps props) {
        return create(props, false, null);
    }

    /**
     * Creates a media clip.
     *
     * @param props the properties
     * @param softSave whether to soft-save
     * @param lang optional language, or {@code null}
     * @return the response
     */
    public SapiResponse create(MediaClipProps props, boolean softSave, String lang) {
        return getSdk().sendRequest("PUT", PATH, new RequestOptions()
                .setQuery(QueryParams.build(param("softsave", softSave), param("lang", lang)))
                .setJson(props));
    }

    /**
     * Updates a media clip.
     *
     * @param id the clip ID
     * @param props the properties
     * @return the response
     */
    @Override
    public SapiResponse update(String id, MediaClipProps props) {
        return update(id, props, false, null);
    }

    /**
     * Updates a media clip.
     *
     * @param id the clip ID
     * @param props the properties
     * @param softSave whether to soft-save
     * @param lang optional language, or {@code null}
     * @return the response
     */
    public SapiResponse update(String id, MediaClipProps props, boolean softSave, String lang) {
        return getSdk().sendRequest("PUT", PATH + "/" + QueryParams.encode(id), new RequestOptions()
                .setQuery(QueryParams.build(param("softsave", softSave), param("lang", lang)))
                .setJson(props));
    }

    /**
     * Updates a media clip.
     *
     * @param id the clip ID
     * @param props the properties
     * @param softSave whether to soft-save
     * @param lang optional language, or {@code null}
     * @return the response
     */
    public SapiResponse update(long id, MediaClipProps props, boolean softSave, String lang) {
        return update(Long.toString(id), props, softSave, lang);
    }

    /**
     * Deletes a media clip.
     *
     * @param id the clip ID
     * @return the response
     */
    @Override
    public SapiResponse delete(String id) {
        return delete(id, false);
    }

    /**
     * Deletes a media clip, optionally purging it.
     *
     * @param id the clip ID
     * @param purge whether to purge
     * @return the response
     */
    public SapiResponse delete(String id, boolean purge) {
        return getSdk().sendRequest("DELETE", PATH + "/" + QueryParams.encode(id), new RequestOptions()
                .setQuery(purge ? QueryParams.build(param("purge", true)) : null));
    }

    /**
     * Deletes a media clip, optionally purging it.
     *
     * @param id the clip ID
     * @param purge whether to purge
     * @return the response
     */
    public SapiResponse delete(long id, boolean purge) {
        return delete(Long.toString(id), purge);
    }

    // -- upload ----------------------------------------------------------------------------

    private static Path requireFile(String mediaClipPath) {
        Path path = Paths.get(mediaClipPath);
        if (!Files.isRegularFile(path)) {
            String message = "File " + mediaClipPath + " is not a file or does not exist.";
            throw new UncheckedIOException(message, new FileNotFoundException(message));
        }
        return path;
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Asks SAPI for presigned upload URLs for a local file.
     *
     * @param mediaClipPath path to the local file
     * @return the response; parse it with {@code json(UploadData.class)}
     * @throws UncheckedIOException (caused by a {@link FileNotFoundException}) if the path is not
     *         an existing file
     */
    public SapiResponse initializeUpload(String mediaClipPath) {
        return initializeUpload(mediaClipPath, null);
    }

    /**
     * Asks SAPI for presigned upload URLs for a local file.
     *
     * @param mediaClipPath path to the local file
     * @param mediaClipId existing clip ID to attach the upload to
     * @return the response; parse it with {@code json(UploadData.class)}
     * @throws UncheckedIOException (caused by a {@link FileNotFoundException}) if the path is not
     *         an existing file
     */
    public SapiResponse initializeUpload(String mediaClipPath, long mediaClipId) {
        return initializeUpload(mediaClipPath, Long.valueOf(mediaClipId));
    }

    private SapiResponse initializeUpload(String mediaClipPath, Long mediaClipId) {
        Path file = requireFile(mediaClipPath);
        return getSdk().sendRequest("GET", PATH + "/0/upload", new RequestOptions().setQuery(QueryParams.build(
                param("filename", file.getFileName().toString()),
                param("filesize", sizeOf(file)),
                param("contenttype", getMimeType(mediaClipPath)),
                param("clipid", mediaClipId))));
    }

    /**
     * Aborts a multi-part upload.
     *
     * @param s3FileKey the S3 object key
     * @param s3UploadId the S3 multi-part upload ID
     * @return the response
     */
    public SapiResponse abortUpload(String s3FileKey, String s3UploadId) {
        return getSdk().sendRequest("PUT", PATH + "/0/abortUpload", new RequestOptions().setQuery(QueryParams.build(
                param("s3filekey", s3FileKey), param("s3uploadid", s3UploadId))));
    }

    /**
     * Completes a multi-part upload.
     *
     * @param s3FileKey the S3 object key
     * @param s3UploadId the S3 multi-part upload ID
     * @param s3Parts the uploaded parts
     * @return the response
     */
    public SapiResponse completeUpload(String s3FileKey, String s3UploadId, Iterable<S3Part> s3Parts) {
        List<S3Part> parts = new ArrayList<>();
        s3Parts.forEach(parts::add);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("s3FileKey", s3FileKey);
        body.put("s3UploadId", s3UploadId);
        body.put("s3Parts", parts);
        return getSdk().sendRequest("PUT", PATH + "/0/completeUpload", new RequestOptions().setJson(body));
    }

    /**
     * Executes a file upload using presigned URLs from the SAPI. Handles both single-chunk and
     * multi-part uploads. Parts are uploaded one after the other, each sent as a sized body (a
     * {@code Content-Length}, never chunked transfer encoding) with only that part held in
     * memory. Every part is attempted; if any part is rejected a multi-part upload is aborted
     * (best effort) before the failure is thrown.
     *
     * @param mediaClipPath path to the local file
     * @param uploadData upload configuration returned by {@link #initializeUpload(String)}
     * @return {@code true} on success
     */
    public boolean executeUpload(String mediaClipPath, UploadData uploadData) {
        requireFile(mediaClipPath);
        Objects.requireNonNull(uploadData, "uploadData");

        if (uploadData.getChunks() == null || uploadData.getPresignedUrls() == null) {
            throw new IllegalArgumentException("uploadData must contain 'chunks' and 'presignedUrls' keys.");
        }

        if (uploadData.getChunks() == 1) {
            SapiResponse response = performUpload(mediaClipPath, uploadData.getPresignedUrls().get(0));
            response.assertOk();
            return true;
        }

        String key = uploadData.getKey();
        String uploadId = uploadData.getUploadId();
        if (key == null || key.isEmpty() || uploadId == null || uploadId.isEmpty()) {
            throw new IllegalArgumentException("uploadData for multi-part uploads must contain 'key' and 'uploadId' keys.");
        }

        List<SapiResponse> responses = new ArrayList<>();
        for (PresignedUrl url : uploadData.getPresignedUrls()) {
            responses.add(performUpload(mediaClipPath, url));
        }
        try {
            SapiResponse.assertAllOk(responses);
        } catch (RuntimeException e) {
            try {
                abortUpload(key, uploadId).assertOk();
            } catch (RuntimeException ignored) {
                // Abort failed; swallow to preserve the original error.
            }
            throw e;
        }

        List<S3Part> parts = new ArrayList<>();
        for (SapiResponse response : responses) {
            String eTag = response.header("ETag");
            String partNumber = response.queryParam("partNumber");
            parts.add(new S3Part(
                    eTag == null ? "" : eTag.replace("\"", ""),
                    partNumber == null ? "" : partNumber));
        }

        completeUpload(key, uploadId, parts).assertOk();
        return true;
    }

    private SapiResponse performUpload(String mediaClipPath, PresignedUrl presignedUrl) {
        Path file = Paths.get(mediaClipPath);
        long fileLength = sizeOf(file);
        long chunkSize = presignedUrl.getChunkSize() != null ? presignedUrl.getChunkSize() : fileLength;
        long offset = presignedUrl.getOffset() != null ? presignedUrl.getOffset() : 0;

        // Read the chunk into a buffer so the PUT carries a Content-Length and no chunked
        // Transfer-Encoding, which the S3 presigned PUT rejects (501 NotImplemented). Only one
        // bounded chunk is held in memory per part.
        long length = Math.max(0, Math.min(chunkSize, fileLength - offset));
        if (length > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("Upload part of " + length + " bytes is too large to buffer.");
        }
        byte[] buffer = new byte[(int) length];
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Timeout zero: an upload's duration scales with file size, so the default request timeout
        // would abort large uploads. The presigned URL is cross-origin, so sendRequest already
        // omits the rpctoken (no auth leak to S3).
        return getSdk().sendRequest("PUT", presignedUrl.getPresignedUrlValue(), new RequestOptions()
                .setBody(HttpRequest.BodyPublishers.ofByteArray(buffer))
                .setTimeout(Duration.ZERO));
    }

    /**
     * Polls S3 once to determine upload progress as a percentage (0-100).
     *
     * @param listPartsUrl S3 URL to list uploaded parts
     * @param headObjectUrl S3 URL to check if the object exists (upload complete)
     * @param partsCount total number of expected parts
     * @return the progress percentage
     */
    public double getUploadProgress(String listPartsUrl, String headObjectUrl, int partsCount) {
        return getUploadProgress(listPartsUrl, headObjectUrl, partsCount, 0);
    }

    /**
     * Polls S3 once to determine upload progress as a percentage (0-100).
     *
     * @param listPartsUrl S3 URL to list uploaded parts
     * @param headObjectUrl S3 URL to check if the object exists (upload complete)
     * @param partsCount total number of expected parts
     * @param requestDelay delay in ms before making the request
     * @return the progress percentage
     */
    public double getUploadProgress(String listPartsUrl, String headObjectUrl, int partsCount, int requestDelay) {
        if (requestDelay > 0) {
            sleep(requestDelay);
        }

        SapiResponse response = getSdk().sendRequest("GET", listPartsUrl);

        if (response.getStatusCode() == 404) {
            SapiResponse headResponse = getSdk().sendRequest("HEAD", headObjectUrl);
            return headResponse.getStatusCode() == 404 ? 0 : 100;
        }

        if (response.getStatusCode() == 200) {
            JsonNode root = response.json();
            JsonNode parts = root instanceof ObjectNode ? root.get("Part") : null;
            int uploadedPartsCount;
            if (parts != null && parts.isArray()) {
                uploadedPartsCount = parts.size();
            } else if (parts != null && parts.isObject()) {
                // S3 returns a single object instead of a 1-element array for one part
                uploadedPartsCount = 1;
            } else {
                uploadedPartsCount = 0;
            }
            return (double) uploadedPartsCount / partsCount * 100;
        }

        response.assertOk();
        return 0; // unreachable, assertOk throws
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CancellationException cancelled = new CancellationException("Interrupted while waiting to poll the upload progress");
            cancelled.initCause(e);
            throw cancelled;
        }
    }

    /**
     * Upload progress as a blocking iterator of percentages (0-100) that ends once the upload is
     * complete (100), with the default poll interval and no iteration limit.
     *
     * @param listPartsUrl S3 URL to list uploaded parts
     * @param headObjectUrl S3 URL to check if the object exists
     * @param partsCount total number of expected parts
     * @return the iterator
     * @see #uploadProgress(String, String, int, int, int)
     */
    public java.util.Iterator<Double> uploadProgress(String listPartsUrl, String headObjectUrl, int partsCount) {
        return uploadProgress(listPartsUrl, headObjectUrl, partsCount, DEFAULT_UPLOAD_PROGRESS_POLL_INTERVAL, 0);
    }

    /**
     * Upload progress as a blocking iterator of percentages (0-100) that ends once the upload is
     * complete (100). Nothing happens until the iterator is used: every {@code next()} waits
     * out the poll interval (relative to the previous poll), polls S3 and returns the
     * percentage; {@code hasNext()} is true until a poll has returned 100 and never blocks.
     *
     * <p>{@code next()} throws {@link IllegalStateException} when {@code maxIterations} polls have
     * been made without reaching 100, and a {@link com.bluebillywig.sapi.exceptions.SapiRequestException}
     * when S3 answers with an unexpected status. If the thread is interrupted while waiting, a
     * {@link CancellationException} is thrown and the interrupt flag restored.
     *
     * @param listPartsUrl S3 URL to list uploaded parts
     * @param headObjectUrl S3 URL to check if the object exists
     * @param partsCount total number of expected parts
     * @param pollInterval minimum interval (ms) between polls
     * @param maxIterations maximum number of polls (0 = unlimited)
     * @return the iterator
     */
    public java.util.Iterator<Double> uploadProgress(
            String listPartsUrl, String headObjectUrl, int partsCount, int pollInterval, int maxIterations) {
        return new java.util.Iterator<>() {
            private double uploadProgress = 0;
            private long timePrevStart = 0;
            private boolean polled = false;
            private int iterations = 0;

            @Override
            public boolean hasNext() {
                return uploadProgress != 100;
            }

            @Override
            public Double next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                if (maxIterations > 0 && iterations >= maxIterations) {
                    throw new IllegalStateException(
                            "Upload progress polling exceeded the maximum of " + maxIterations + " iterations.");
                }

                long timeStart = System.nanoTime() / 1_000_000L;
                int delayTime = polled ? (int) Math.max(0, pollInterval - (timeStart - timePrevStart)) : 0;

                uploadProgress = getUploadProgress(listPartsUrl, headObjectUrl, partsCount, delayTime);
                timePrevStart = timeStart;
                polled = true;
                iterations++;
                return uploadProgress;
            }
        };
    }

    // -- source path -----------------------------------------------------------------------

    /**
     * Retrieves the absolute source video path for a media clip.
     *
     * @param mediaClipId the media clip ID
     * @return the absolute URL
     */
    public String getSourcePath(String mediaClipId) {
        return getSourcePath(mediaClipId, true);
    }

    /**
     * Retrieves the source video path for a media clip.
     *
     * @param mediaClipId the media clip ID
     * @param absolute if {@code true}, returns an absolute URL using the publication's default
     *        media asset path
     * @return the path
     * @throws IllegalStateException when the clip has no {@code src} (or the publication no
     *         {@code defaultMediaAssetPath})
     */
    public String getSourcePath(String mediaClipId, boolean absolute) {
        SapiResponse response = get(mediaClipId, null, true);
        response.assertOk();
        JsonNode data = response.json();
        JsonNode srcNode = data instanceof ObjectNode ? data.get("src") : null;
        if (srcNode == null || !srcNode.isValueNode() || srcNode.isNull()) {
            throw new IllegalStateException("MediaClip response does not contain a 'src' field.");
        }
        String src = srcNode.asText();
        return absolute ? getAbsoluteVideoPath(src) : src;
    }

    /**
     * Retrieves the absolute source video path for a media clip.
     *
     * @param mediaClipId the media clip ID
     * @return the absolute URL
     */
    public String getSourcePath(long mediaClipId) {
        return getSourcePath(Long.toString(mediaClipId), true);
    }

    /**
     * Retrieves the source video path for a media clip.
     *
     * @param mediaClipId the media clip ID
     * @param absolute if {@code true}, returns an absolute URL
     * @return the path
     */
    public String getSourcePath(long mediaClipId, boolean absolute) {
        return getSourcePath(Long.toString(mediaClipId), absolute);
    }

    /**
     * Converts a relative video path to an absolute URL using publication data.
     *
     * @param relativeVideoPath the relative path (with or without leading slash)
     * @return the absolute URL
     * @throws IllegalStateException when the publication data has no {@code defaultMediaAssetPath}
     */
    public String getAbsoluteVideoPath(String relativeVideoPath) {
        ObjectNode publicationData = getSdk().getPublicationData();
        JsonNode dmapNode = publicationData.get("defaultMediaAssetPath");
        String dmap = dmapNode == null || dmapNode.isNull() ? null : dmapNode.asText();
        if (dmap == null || dmap.isEmpty()) {
            throw new IllegalStateException("Publication data missing 'defaultMediaAssetPath'.");
        }
        int start = 0;
        while (start < relativeVideoPath.length() && relativeVideoPath.charAt(start) == '/') {
            start++;
        }
        return dmap + "/" + relativeVideoPath.substring(start);
    }
}
