# Blue Billywig SAPI Java SDK

This Java SDK provides abstractions to interact with the Blue Billywig Server API (SAPI).
It is a port of the [.NET SDK](https://github.com/bluebillywig/bb-sapi-dotnet-sdk), itself a
port of the [Node.js SDK](https://github.com/bluebillywig/bb-sapi-node-sdk), and mirrors their
design: the same entities, the same `SapiResponse` shape, the same HOTP RPC-token
authentication and the same upload flow.

**All methods are blocking.** There is no `Async` variant and no `CompletableFuture` in the API:
a call returns when the response has been read (or throws). Run calls on your own executor or
virtual threads if you need concurrency. An `Sdk` instance is thread-safe and meant to be shared.

## Requirements

- Java 17 or newer
- One runtime dependency: `jackson-databind`. HTTP is `java.net.http.HttpClient`.

## Installation

```xml
<dependency>
  <groupId>com.bluebillywig</groupId>
  <artifactId>bb-sapi-java-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.bluebillywig:bb-sapi-java-sdk:1.0.0'
```

## Quick Start

```java
import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.SapiResponse;

Sdk sdk = Sdk.withRpcTokenAuthentication(
    "my-publication",
    1,               // token ID
    "shared-secret"  // shared secret
);

// List media clips
SapiResponse response = sdk.mediaClip().list();
System.out.println(response.json());
```

## Authentication

The SDK uses HOTP-based RPC token authentication. You need a **token ID** and **shared secret**
from your Blue Billywig publication settings.

```java
// Recommended: use the convenience factory
Sdk sdk = Sdk.withRpcTokenAuthentication("my-publication", tokenId, sharedSecret);

// Or provide a custom authenticator
import com.bluebillywig.sapi.authentication.RpcTokenAuthenticator;

RpcTokenAuthenticator authenticator = new RpcTokenAuthenticator(tokenId, sharedSecret);
Sdk sdk = new Sdk("my-publication", authenticator);
```

**Clock synchronization**: The RPC token is time-based (HOTP). Both client and server clocks
must be reasonably synchronized (within the token expiration window, default 120 seconds).
Significant clock drift will cause authentication failures.

## Entities

All entities support standard CRUD operations where applicable. IDs are accepted as `long` or
`String`.

| Entity | List | Get | Create | Update | Delete | Search |
|---|---|---|---|---|---|---|
| `sdk.mediaClip()` | Yes | Yes | Yes | Yes | Yes | Yes |
| `sdk.playlist()` | Yes | Yes | Yes | Yes | Yes | - |
| `sdk.channel()` | Yes | Yes | Yes | Yes | Yes | - |
| `sdk.playout()` | Yes | Yes | Yes | Yes | Yes | - |
| `sdk.subtitle()` | Yes | Yes | Yes | Yes | Yes | - |

`sdk.mediaClipList()` is the legacy alias for `sdk.playlist()`. `sdk.thumbnail()` is not a CRUD
entity: SAPI has no thumbnail records, only image routes, so it provides URL helpers (see
[Thumbnails](#thumbnails)).

### Media Clips

```java
import com.bluebillywig.sapi.types.MediaClipProps;

// List
SapiResponse response = sdk.mediaClip().list(15, 0, "createddate desc");

// Get (with optional language and job inclusion)
response = sdk.mediaClip().get(123);
response = sdk.mediaClip().get(123, "en", false);

// Create
response = sdk.mediaClip().create(new MediaClipProps().setTitle("My Video"));

// Update
response = sdk.mediaClip().update(123, new MediaClipProps().setTitle("Updated Title"));

// Delete (with optional purge)
response = sdk.mediaClip().delete(123);
response = sdk.mediaClip().delete(123, true);
```

Every `*Props` class has additional properties (`getAdditionalProperties()` /
`setAdditionalProperties(map)`) for fields the SDK does not model; they are sent verbatim.
Properties you do not set (`null`) are omitted from the request.

### Search (filtersets)

`list` can only page and sort. `search` filters, using the same filterset structure
the OVP builds in its filter UI, so a filterset moves between the OVP, the API and this SDK
unchanged. Groups are AND-ed, filters within a group OR-ed.

```java
import com.bluebillywig.sapi.search.Filter;
import com.bluebillywig.sapi.search.FilterOperator;
import com.bluebillywig.sapi.search.FilterSet;

FilterSet filterSet = FilterSet.create()
    .where("status", FilterOperator.IS, "published")
    .where("title", FilterOperator.CONTAINS, "koert");

SapiResponse response = sdk.mediaClip().search(filterSet);

// OR within a group
FilterSet either = FilterSet.create().andGroup(
    new Filter("status", FilterOperator.IS, "published"),
    new Filter("status", FilterOperator.IS, "draft"));

// Ingest a filterset saved by the OVP (bare list or SearchRequest envelope)
FilterSet fromOvp = FilterSet.from(json);
```

The filterset is sent as JSON and compiled by SAPI; it is deliberately not compiled
client-side. Numbers and booleans are normalised to strings on the wire, presence operators
(`IS_EMPTY` / `IS_NOT_EMPTY`) carry the `*` placeholder the backend requires, and filters with a
blank value are dropped. Raw Solr filters can be passed through the `filterQueries` argument
(`search(filterSet, limit, offset, sort, query, filterQueries)`); they go out as indexed `fq[0]`
parameters, the only form SAPI reads.

### Playlists, Channels, Playouts, Subtitles

```java
// All follow the same pattern
SapiResponse response = sdk.playlist().list();
response = sdk.playlist().get(1);
response = sdk.playlist().create(new PlaylistProps().setTitle("My Playlist"));
response = sdk.playlist().update(1, new PlaylistProps().setTitle("Updated"));
response = sdk.playlist().delete(1);
```

## File Uploads

The SDK supports single-chunk and multi-part uploads to S3 via presigned URLs. Each part is
sent as a sized body (`Content-Length`, no chunked transfer encoding), one part in memory at a
time. Parts are uploaded one after the other (the .NET SDK uploads them in parallel).

```java
import com.bluebillywig.sapi.types.UploadData;
import java.util.Iterator;

// 1. Initialize the upload
SapiResponse initResponse = sdk.mediaClip().initializeUpload("/path/to/video.mp4");
initResponse.assertOk();
UploadData uploadData = initResponse.json(UploadData.class);

// 2. Execute the upload
boolean success = sdk.mediaClip().executeUpload("/path/to/video.mp4", uploadData);

// 3. (Optional) Track upload progress
Iterator<Double> progress = sdk.mediaClip().uploadProgress(
    uploadData.getListPartsUrl(), uploadData.getHeadObjectUrl(), uploadData.getChunks());
while (progress.hasNext()) {
    System.out.println("Upload progress: " + progress.next() + "%");
}
```

### Upload progress model

Upload progress (`IAsyncEnumerable<double>` in .NET) is a **blocking `Iterator<Double>`**.
Percentages are `Double` because they are fractional (1 of 3 parts is 33.3). The iterator is
lazy: every `next()` waits out the poll interval, polls S3 once and returns the percentage;
`hasNext()` never blocks and turns `false` after a poll returned `100`. `next()` throws
`IllegalStateException` when `maxIterations` polls were made without reaching 100 (the .NET
`InvalidOperationException`). For a single poll use `getUploadProgress(...)`.

## Thumbnails

```java
// Absolute thumbnail URL with dimensions
String url = sdk.thumbnail().getAbsoluteImagePath("/path/to/image.jpg", 640, 360);
// => https://my-publication.bbvms.com/image/640/360/path/to/image.jpg

// A media clip's poster (never build this from clip.src: that is the source media file)
String poster = sdk.thumbnail().getMediaClipPosterPath(1234, 320, 180);
// => https://my-publication.bbvms.com/mediaclip/1234/spthumbnail/320/180.webp

// Draft clips need a READ-ONLY RPC token (the URL ends up in page source, and is short-lived)
String draftPoster = sdk.thumbnail().getMediaClipPosterPath(1234, null, null, "12-...");
```

## Response Handling

All entity methods return a `SapiResponse`:

```java
SapiResponse response = sdk.mediaClip().get(123);

// Check status
if (response.isOk()) {
    JsonNode node = response.json();                 // Jackson tree, null for an empty body
    MyType typed = response.json(MyType.class);      // deserialized with Jackson
}

// Or assert (throws on non-2xx)
response.assertOk();

// Access response details
response.getStatusCode();       // e.g. 200
response.getBody();             // raw body string
response.header("Content-Type");
response.queryParam("limit");   // query param from the request URL
response.getStatusCategory();   // HttpStatusCodeCategory enum

// Batch response utilities
SapiResponse.allOk(responses);
SapiResponse.assertAllOk(responses);
SapiResponse.failedResponses(responses);
```

`json()` throws an `UncheckedIOException` (caused by Jackson's `JsonProcessingException`) when
the body is not valid JSON. `getStatusText()` is the standard reason phrase for the status code,
because `java.net.http` does not expose the phrase from the wire.

## Error Handling

The SDK throws typed, unchecked exceptions. The Node SDK's `HTTP*Exception` names are prefixed
`Sapi` here, as in the .NET SDK.

| Node SDK | Java SDK |
|---|---|
| `HTTPRequestException` | `SapiRequestException` |
| `HTTPClientErrorException` (4xx) | `SapiClientErrorException` |
| `HTTPServerErrorException` (5xx) | `SapiServerErrorException` |
| `HTTPConnectionException` (transport / timeout) | `SapiConnectionException` |

```java
import com.bluebillywig.sapi.exceptions.*;

try {
    SapiResponse response = sdk.mediaClip().get(999);
    response.assertOk();
} catch (SapiClientErrorException e) {
    System.err.println("Client error " + e.getStatusCode() + ": " + e.getMessage());
    System.err.println("Response body: " + e.getResponseBody());
} catch (SapiServerErrorException e) {
    System.err.println("Server error " + e.getStatusCode() + ": " + e.getMessage());
} catch (SapiConnectionException e) {
    // No response received: network failure or timeout. getStatusCode() is 0, getCause() has the cause.
}
```

Other failures map to the usual unchecked exceptions: `IllegalArgumentException` (bad
arguments, unknown filter operator), `IllegalStateException` (a clip without `src`),
`UncheckedIOException` (a missing upload file, wrapping a `FileNotFoundException`; invalid JSON).
If the calling thread is interrupted during a request, the request is cancelled, the interrupt
flag is restored and a `java.util.concurrent.CancellationException` is thrown (the counterpart of
a cancelled `CancellationToken`); it is not wrapped in a `SapiConnectionException`.

## Configuration

```java
import java.time.Duration;

Sdk sdk = Sdk.withRpcTokenAuthentication("my-publication", tokenId, sharedSecret, new SdkOptions()
    // Override the base URI (useful for testing or custom deployments)
    .setBaseUri("https://custom-host.example.com")

    // Inject an HttpClient (testing, proxy/TLS configuration, a shared client)
    .setHttpClient(myHttpClient)

    // Default per-request timeout (Duration.ZERO disables). Default 30 seconds.
    .setTimeout(Duration.ofSeconds(30)));
```

The timeout covers the whole exchange, including reading the response body. The SDK waits on
`HttpClient.sendAsync(...)` to enforce it, so an injected client must implement `sendAsync`.
The client the SDK creates does not follow redirects, so the RPC token cannot be replayed to
another origin; configure your own client if you want different behaviour.

Authentication headers are attached only to same-origin (SAPI) requests; cross-origin URLs
such as presigned S3 upload and progress URLs never receive the RPC token.

## Development

```bash
mvn -B verify
```

The tests talk to real local HTTP servers (`com.sun.net.httpserver.HttpServer` on 127.0.0.1), so
Content-Length, query encoding and headers are verified on the wire.

`src/test/java/com/bluebillywig/sapi/tools/AccSmoke.java` is a small runnable smoke test against a
real publication (credentials from `SAPI_AUTH_KEY`, see the comments at the top of the file). It is
compiled with the tests and is not part of the published jar.
