package com.bluebillywig.sapi;

import com.bluebillywig.sapi.authentication.Authenticator;
import com.bluebillywig.sapi.authentication.RpcTokenAuthenticator;
import com.bluebillywig.sapi.entities.Channel;
import com.bluebillywig.sapi.entities.MediaClip;
import com.bluebillywig.sapi.entities.Playlist;
import com.bluebillywig.sapi.entities.Playout;
import com.bluebillywig.sapi.entities.Subtitle;
import com.bluebillywig.sapi.entities.Thumbnail;
import com.bluebillywig.sapi.exceptions.SapiConnectionException;
import com.bluebillywig.sapi.exceptions.SapiRequestException;
import com.bluebillywig.sapi.util.HttpStatusText;
import com.bluebillywig.sapi.util.QueryParams;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main entry point for the Blue Billywig SAPI SDK. Provides authenticated access to SAPI
 * entities and handles HTTP communication.
 *
 * <p>All methods are blocking. An instance is thread-safe and meant to be shared.
 */
public final class Sdk {
    private static final List<EntityRegistration> ENTITY_REGISTRATIONS = List.of(
            new EntityRegistration("mediaclip", MediaClip::new),
            new EntityRegistration("mediacliplist", Playlist::new),
            new EntityRegistration("playlist", Playlist::new),
            new EntityRegistration("channel", Channel::new),
            new EntityRegistration("playout", Playout::new),
            new EntityRegistration("subtitle", Subtitle::new),
            new EntityRegistration("thumbnail", Thumbnail::new));

    private final Authenticator authenticator;
    private final HttpClient http;
    private final Duration timeout;
    private final Object publicationLock = new Object();
    private volatile ObjectNode publicationData;

    private final String publication;
    private final String baseUri;
    private final EntityRegister entities;

    /**
     * Creates an SDK instance.
     *
     * @param publication the publication name
     * @param authenticator the request authenticator
     */
    public Sdk(String publication, Authenticator authenticator) {
        this(publication, authenticator, null);
    }

    /**
     * Creates an SDK instance.
     *
     * @param publication the publication name
     * @param authenticator the request authenticator
     * @param options optional SDK configuration, or {@code null}
     */
    public Sdk(String publication, Authenticator authenticator, SdkOptions options) {
        this.publication = Objects.requireNonNull(publication, "publication");
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        SdkOptions opts = options == null ? new SdkOptions() : options;
        this.baseUri = opts.getBaseUri() != null ? opts.getBaseUri() : "https://" + publication + ".bbvms.com";
        this.timeout = opts.getTimeout();
        // Redirects are deliberately not followed: java.net.http would replay custom headers
        // (the rpctoken) to whatever host a redirect points at.
        this.http = opts.getHttpClient() != null
                ? opts.getHttpClient()
                : HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        this.entities = new EntityRegister(this, ENTITY_REGISTRATIONS);
    }

    /**
     * Creates an SDK instance configured with RPC token authentication.
     *
     * @param publication the publication name
     * @param tokenId the RPC token ID
     * @param sharedSecret the shared secret for HOTP token generation
     * @return the SDK
     */
    public static Sdk withRpcTokenAuthentication(String publication, int tokenId, String sharedSecret) {
        return withRpcTokenAuthentication(publication, tokenId, sharedSecret, null);
    }

    /**
     * Creates an SDK instance configured with RPC token authentication.
     *
     * @param publication the publication name
     * @param tokenId the RPC token ID
     * @param sharedSecret the shared secret for HOTP token generation
     * @param options optional SDK configuration, or {@code null}
     * @return the SDK
     */
    public static Sdk withRpcTokenAuthentication(String publication, int tokenId, String sharedSecret, SdkOptions options) {
        return new Sdk(publication, new RpcTokenAuthenticator(tokenId, sharedSecret), options);
    }

    /** @return the publication name */
    public String getPublication() {
        return publication;
    }

    /** @return the base URI used for resolving relative SAPI paths */
    public String getBaseUri() {
        return baseUri;
    }

    /** @return the entity register; entities are created lazily and cached */
    public EntityRegister getEntities() {
        return entities;
    }

    /** @return the MediaClip entity for CRUD operations on media clips */
    public MediaClip mediaClip() {
        return entities.get("mediaclip", MediaClip.class);
    }

    /** @return the Playlist entity via the legacy {@code mediacliplist} alias */
    public Playlist mediaClipList() {
        return entities.get("mediacliplist", Playlist.class);
    }

    /** @return the Playlist entity for CRUD operations on playlists */
    public Playlist playlist() {
        return entities.get("playlist", Playlist.class);
    }

    /** @return the Channel entity for CRUD operations on channels */
    public Channel channel() {
        return entities.get("channel", Channel.class);
    }

    /** @return the Playout entity for CRUD operations on playouts */
    public Playout playout() {
        return entities.get("playout", Playout.class);
    }

    /** @return the Subtitle entity for CRUD operations on subtitles */
    public Subtitle subtitle() {
        return entities.get("subtitle", Subtitle.class);
    }

    /** @return the Thumbnail entity for thumbnail path resolution */
    public Thumbnail thumbnail() {
        return entities.get("thumbnail", Thumbnail.class);
    }

    /**
     * True when {@code url} targets the same origin as the SAPI base URI. Used to gate
     * auth-header attachment: cross-origin URLs (presigned S3/CloudFront upload and progress URLs)
     * must never receive the rpctoken, or the credential ends up in CDN access logs. An
     * unparseable URL is treated as cross-origin (fail closed).
     */
    private boolean isSameOrigin(String url) {
        URI target;
        URI origin;
        try {
            target = new URI(url);
            origin = new URI(baseUri);
        } catch (URISyntaxException e) {
            return false;
        }
        if (!target.isAbsolute() || !origin.isAbsolute() || target.getHost() == null || origin.getHost() == null) {
            return false;
        }
        return target.getScheme().equalsIgnoreCase(origin.getScheme())
                && target.getHost().equalsIgnoreCase(origin.getHost())
                && effectivePort(target) == effectivePort(origin);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        return switch (scheme) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    /**
     * Sends an authenticated HTTP request to the SAPI with default options. Relative paths are
     * resolved against the SDK's base URI.
     *
     * @param method HTTP method (GET, PUT, DELETE, etc.)
     * @param path URL path or absolute URL
     * @return the response; non-2xx statuses are returned, not thrown (see
     *         {@link SapiResponse#assertOk()})
     * @throws SapiConnectionException on transport failure or timeout
     */
    public SapiResponse sendRequest(String method, String path) {
        return sendRequest(method, path, null);
    }

    /**
     * Sends an authenticated HTTP request to the SAPI. Relative paths are resolved against the
     * SDK's base URI. The call blocks until the whole response body has been read.
     *
     * <p>If the calling thread is interrupted while waiting, the request is cancelled, the
     * thread's interrupt flag is restored and a {@link CancellationException} (not a
     * {@link SapiRequestException}) is thrown, the counterpart of a cancelled {@code CancellationToken}
     * in the .NET SDK.
     *
     * @param method HTTP method (GET, PUT, DELETE, etc.)
     * @param path URL path or absolute URL
     * @param options optional query parameters, JSON body, raw body, or extra headers; may be
     *        {@code null}
     * @return the response; non-2xx statuses are returned, not thrown (see
     *         {@link SapiResponse#assertOk()})
     * @throws SapiConnectionException on transport failure or timeout
     */
    public SapiResponse sendRequest(String method, String path, RequestOptions options) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        RequestOptions opts = options == null ? new RequestOptions() : options;

        // Resolve URL first: relative paths get resolved against BaseUri, and the resolved
        // origin decides whether auth headers may be attached.
        String lowerPath = path.toLowerCase(Locale.ROOT);
        String url = lowerPath.startsWith("http://") || lowerPath.startsWith("https://") ? path : baseUri + path;

        if (opts.getQuery() != null && !opts.getQuery().isEmpty()) {
            url = QueryParams.append(url, opts.getQuery());
        }

        // Attach SAPI auth ONLY to same-origin requests. Presigned S3/CloudFront upload +
        // progress URLs are cross-origin; sending the rpctoken there leaks the credential into
        // CDN access logs. SkipAuth can also suppress it on a same-origin request.
        boolean skipAuth = opts.getSkipAuth() != null ? opts.getSkipAuth() : !isSameOrigin(url);
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!skipAuth) {
            headers.putAll(authenticator.authenticate());
        }
        if (opts.getHeaders() != null) {
            headers.putAll(opts.getHeaders());
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new SapiConnectionException("Network request to " + url + " failed: invalid URL", e);
        }

        String upperMethod = method.toUpperCase(Locale.ROOT);
        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
            if (opts.getJson() != null) {
                body = HttpRequest.BodyPublishers.ofString(SapiJson.write(opts.getJson()), StandardCharsets.UTF_8);
                headers.putIfAbsent("Content-Type", "application/json");
            } else if (opts.getBody() != null) {
                body = opts.getBody();
            }
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            request = builder.method(upperMethod, body).build();
        } catch (IllegalArgumentException e) {
            throw new SapiConnectionException("Network request to " + url + " failed: " + describe(e), e);
        }

        // Per-request timeout (SdkOptions default, overridable; zero disables). Without this a
        // stalled connection hangs the call forever. It bounds the whole exchange including the
        // body read.
        Duration effectiveTimeout = opts.getTimeout() != null ? opts.getTimeout() : timeout;
        boolean hasTimeout = !effectiveTimeout.isZero() && !effectiveTimeout.isNegative();

        CompletableFuture<HttpResponse<byte[]>> future;
        HttpResponse<byte[]> response;
        try {
            future = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (RuntimeException e) {
            throw wrapTransport(url, e);
        }
        try {
            response = hasTimeout ? future.get(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS) : future.get();
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SapiConnectionException(
                    "Request to " + url + " timed out after " + effectiveTimeout.toMillis() + "ms", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            CancellationException cancelled = new CancellationException("Request to " + url + " was interrupted");
            cancelled.initCause(e);
            throw cancelled;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof SapiRequestException sapi) {
                throw sapi;
            }
            throw wrapTransport(url, cause);
        } catch (CancellationException e) {
            throw wrapTransport(url, e);
        }

        // Wrap transport errors (network failure, DNS, reset) in the SDK exception hierarchy
        // instead of letting a raw IOException escape.
        Map<String, String> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> header : response.headers().map().entrySet()) {
            responseHeaders.put(header.getKey(), String.join(", ", header.getValue()));
        }
        String bodyText = new String(response.body(), StandardCharsets.UTF_8);
        int status = response.statusCode();
        return new SapiResponse(url, upperMethod, status, HttpStatusText.of(status), responseHeaders, bodyText);
    }

    private static SapiConnectionException wrapTransport(String url, Throwable cause) {
        return new SapiConnectionException("Network request to " + url + " failed: " + describe(cause), cause);
    }

    private static String describe(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.getClass().getName();
    }

    /**
     * Fetches and caches publication data from the SAPI. Subsequent calls return the cached
     * result; concurrent first calls fetch it once. A failed fetch is not cached, so the next
     * call retries.
     *
     * @return the publication data
     * @throws SapiRequestException when the fetch fails or the data is not a JSON object
     */
    public ObjectNode getPublicationData() {
        ObjectNode cached = publicationData;
        if (cached != null) {
            return cached;
        }
        synchronized (publicationLock) {
            if (publicationData == null) {
                publicationData = fetchPublicationData();
            }
            return publicationData;
        }
    }

    private ObjectNode fetchPublicationData() {
        SapiResponse response = sendRequest("GET", "/sapi/publication");
        response.assertOk();
        JsonNode json = response.json();
        if (json instanceof ObjectNode object) {
            return object;
        }
        throw new SapiRequestException("Publication data is not a JSON object.", response.getStatusCode(), response.getBody());
    }
}
