package com.bluebillywig.sapi;

import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.ok;
import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.authentication.Authenticator;
import com.bluebillywig.sapi.authentication.EmptyAuthenticator;
import com.bluebillywig.sapi.entities.Channel;
import com.bluebillywig.sapi.entities.MediaClip;
import com.bluebillywig.sapi.entities.Playlist;
import com.bluebillywig.sapi.entities.Playout;
import com.bluebillywig.sapi.entities.Subtitle;
import com.bluebillywig.sapi.entities.Thumbnail;
import com.bluebillywig.sapi.exceptions.SapiClientErrorException;
import com.bluebillywig.sapi.exceptions.SapiConnectionException;
import com.bluebillywig.sapi.exceptions.SapiRequestException;
import com.bluebillywig.sapi.exceptions.SapiServerErrorException;
import com.bluebillywig.sapi.helpers.MockServer;
import com.bluebillywig.sapi.helpers.SdkTestBase;
import com.bluebillywig.sapi.helpers.StubHttpClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SdkTest extends SdkTestBase {
    @Test
    void addsRpctokenHeaderWithRpcTokenAuthentication() {
        Fixture f = createWithRpcToken(ok());

        f.sdk().sendRequest("GET", "/sapi/test-method");

        assertEquals(1, f.server().calls().size());
        assertTrue(f.server().call(0).header("rpctoken").matches("^1-.+"));
    }

    @Test
    void returnsSapiResponse() {
        Fixture f = create(ok());
        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/test-method");
        response.assertOk();
    }

    @Test
    void handles404() {
        Fixture f = create(status(404));
        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/test-method");
        SapiClientErrorException ex = assertThrows(SapiClientErrorException.class, response::assertOk);
        assertEquals("Not Found", ex.getMessage());
    }

    @Test
    void handles500() {
        Fixture f = create(status(500));
        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/test-method");
        assertThrows(SapiServerErrorException.class, response::assertOk);
    }

    @Test
    void resolvesRelativeUrisAgainstBaseUri() {
        // Without a baseUri override the base is https://{publication}.bbvms.com; with one, relative
        // paths are appended to it. The wire assertion runs against the local server.
        assertEquals("https://my-publication.bbvms.com", offlineSdk().getBaseUri());

        Fixture f = create(ok());
        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/test-method");
        assertEquals(f.server().url("/sapi/test-method"), f.server().call(0).url());
        assertEquals(f.server().url("/sapi/test-method"), response.getUrl());
    }

    @Test
    void doesNotModifyAbsoluteUris() {
        Fixture f = create();
        MockServer other = server(ok());

        SapiResponse response = f.sdk().sendRequest("GET", other.url("/"));

        assertEquals(other.url("/"), other.call(0).url());
        assertEquals(other.url("/"), response.getUrl());
        assertTrue(f.server().calls().isEmpty());
    }

    @Test
    void getsAndCachesPublicationData() {
        Fixture f = create(ok("{\"name\":\"my-publication\"}"));

        ObjectNode data = f.sdk().getPublicationData();
        f.sdk().getPublicationData();

        assertEquals("my-publication", data.get("name").asText());
        assertEquals(1, f.server().calls().size());
        assertEquals(f.server().url("/sapi/publication"), f.server().call(0).url());
    }

    @Test
    void doesNotCacheFailedPublicationFetch() {
        Fixture f = create(status(500), ok("{\"name\":\"p\"}"));

        assertThrows(SapiServerErrorException.class, () -> f.sdk().getPublicationData());
        ObjectNode data = f.sdk().getPublicationData();

        assertEquals("p", data.get("name").asText());
        assertEquals(2, f.server().calls().size());
    }

    @Test
    void sendsJsonBody() {
        Fixture f = create(ok());
        f.sdk().sendRequest("PUT", "/sapi/mediaclip", new RequestOptions().setJson(Map.of("title", "Test")));
        assertEquals("{\"title\":\"Test\"}", f.server().call(0).bodyText());
        assertEquals("application/json", f.server().call(0).header("Content-Type"));
    }

    @Test
    void sendsQueryParams() {
        Fixture f = create(ok());
        Map<String, String> query = new java.util.LinkedHashMap<>();
        query.put("limit", "15");
        query.put("offset", "0");
        f.sdk().sendRequest("GET", "/sapi/mediaclip", new RequestOptions().setQuery(query));
        assertEquals("15", query(f.server().call(0).url(), "limit"));
        assertEquals("0", query(f.server().call(0).url(), "offset"));
    }

    @Test
    void extraHeadersOverrideAuthHeaders() {
        Fixture f = createWithRpcToken(ok());
        f.sdk().sendRequest("GET", "/sapi/x", new RequestOptions().setHeaders(Map.of("rpctoken", "override", "X-Custom", "1")));
        assertEquals("override", f.server().call(0).header("rpctoken"));
        assertEquals("1", f.server().call(0).header("X-Custom"));
    }

    @Test
    void exposesEntitiesTypedAndCached() {
        Sdk sdk = offlineSdk();
        assertInstanceOf(MediaClip.class, sdk.mediaClip());
        assertInstanceOf(Playlist.class, sdk.playlist());
        assertInstanceOf(Playlist.class, sdk.mediaClipList());
        assertInstanceOf(Channel.class, sdk.channel());
        assertInstanceOf(Playout.class, sdk.playout());
        assertInstanceOf(Subtitle.class, sdk.subtitle());
        assertInstanceOf(Thumbnail.class, sdk.thumbnail());
        assertSame(sdk.mediaClip(), sdk.mediaClip());
        assertSame(sdk.thumbnail(), sdk.thumbnail());
        assertSame(sdk, sdk.mediaClip().getSdk());
    }

    @Test
    void entityRegisterByName() {
        Sdk sdk = offlineSdk();
        assertTrue(sdk.getEntities().contains("mediaclip"));
        assertFalse(sdk.getEntities().contains("nonexistent"));
        assertTrue(sdk.getEntities().names().contains("mediaclip"));
        assertTrue(sdk.getEntities().names().contains("playlist"));
        assertTrue(sdk.getEntities().names().contains("thumbnail"));
        assertNull(sdk.getEntities().get("nonexistent"));
        assertNull(sdk.getEntities().get("nonexistent", Entity.class));
        assertSame(sdk.mediaClip(), sdk.getEntities().get("mediaclip"));
        assertInstanceOf(Playlist.class, sdk.getEntities().get("mediacliplist", Playlist.class));
        assertNull(sdk.getEntities().get("mediaclip", Playlist.class));
    }

    // Beyond the .NET suite.

    @Test
    void publicationDataIsFetchedOnceUnderConcurrentFirstUse() throws Exception {
        Fixture f = create(ok("{\"name\":\"p\"}").delayed(150));
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<ObjectNode>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return f.sdk().getPublicationData();
                }));
            }
            start.countDown();

            ObjectNode first = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<ObjectNode> future : futures) {
                assertSame(first, future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, f.server().calls().size());
    }

    @Test
    void statusTextIsDerivedFromTheStatusCode() {
        Fixture f = create(status(404), status(503), status(299));
        assertEquals("Not Found", f.sdk().sendRequest("GET", "/a").getStatusText());
        assertEquals("Service Unavailable", f.sdk().sendRequest("GET", "/b").getStatusText());
        assertEquals("", f.sdk().sendRequest("GET", "/c").getStatusText());
    }

    @Test
    void responseHeadersAreCaseInsensitive() {
        Fixture f = create(MockServer.MockResponse.withHeader("ETag", "\"abc\""));
        SapiResponse response = f.sdk().sendRequest("GET", "/a");
        assertEquals("\"abc\"", response.header("etag"));
        assertEquals("\"abc\"", response.header("ETag"));
    }

    @Test
    void redirectsAreNotFollowedSoTheTokenCannotLeakToAnotherOrigin() {
        MockServer elsewhere = server(ok());
        MockServer.MockResponse redirect = new MockServer.MockResponse(302, Map.of("Location", elsewhere.url("/stolen")), null, 0);
        Fixture f = createWithRpcToken(redirect);

        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/mediaclip");

        assertEquals(302, response.getStatusCode());
        assertThrows(SapiRequestException.class, response::assertOk);
        assertTrue(elsewhere.calls().isEmpty());
    }

    @Test
    void connectionRefusedIsWrappedAsConnectionException() {
        MockServer gone = new MockServer();
        String baseUri = gone.baseUri();
        gone.close();
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(), new SdkOptions().setBaseUri(baseUri));

        SapiConnectionException ex = assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

        assertEquals(0, ex.getStatusCode());
        assertTrue(ex.getCause() instanceof IOException, String.valueOf(ex.getCause()));
    }

    @Test
    void jsonBodyBeatsRawBodyAndGetsAContentLength() {
        Fixture f = create(ok());
        f.sdk().sendRequest("PUT", "/sapi/x", new RequestOptions()
                .setJson(Map.of("a", 1))
                .setBody(HttpRequest.BodyPublishers.ofString("ignored")));
        assertEquals("{\"a\":1}", f.server().call(0).bodyText());
        assertEquals("7", f.server().call(0).header("Content-Length"));
        assertNull(f.server().call(0).header("Transfer-Encoding"));
    }

    @Test
    void invalidMethodOrHeaderIsWrappedAsConnectionException() {
        Fixture f = create(ok());
        assertThrows(SapiConnectionException.class, () -> f.sdk().sendRequest("GET", "/x",
                new RequestOptions().setHeaders(Map.of("Content-Length", "5"))));
        assertThrows(SapiConnectionException.class, () -> f.sdk().sendRequest("GET", "ftp://127.0.0.1/x"));
        assertTrue(f.server().calls().isEmpty());
    }

    // -- SdkRequestHardeningTests ----------------------------------------------------------

    private static final String S3_PATH = "/upload?X-Amz-Signature=abc";

    private static final class CountingAuthenticator implements Authenticator {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public Map<String, String> authenticate() {
            calls.incrementAndGet();
            return Map.of("rpctoken", "1-token");
        }
    }

    @Test
    void doesNotSendRpctokenToCrossOriginUrl() {
        Fixture f = createWithRpcToken();
        MockServer s3 = server(ok());

        f.sdk().sendRequest("PUT", s3.url(S3_PATH));

        assertNull(s3.call(0).header("rpctoken"));
        assertEquals("/upload?X-Amz-Signature=abc", s3.call(0).url().substring(s3.baseUri().length()));
    }

    @Test
    void sendsRpctokenToSameOriginRelativeAndAbsolute() {
        Fixture f = createWithRpcToken(ok(), ok());
        f.sdk().sendRequest("GET", "/sapi/mediaclip");
        f.sdk().sendRequest("GET", f.server().url("/sapi/mediaclip"));
        assertTrue(f.server().call(0).header("rpctoken").startsWith("1-"));
        assertTrue(f.server().call(1).header("rpctoken").startsWith("1-"));
    }

    @Test
    void sameHostOnAnotherPortIsAnotherOrigin() {
        Fixture f = createWithRpcToken();
        MockServer samehost = server(ok());
        f.sdk().sendRequest("GET", samehost.url("/sapi/mediaclip"));
        assertNull(samehost.call(0).header("rpctoken"));
    }

    @Test
    void skipAuthSuppressesAuthOnSameOrigin() {
        Fixture f = createWithRpcToken(ok());
        f.sdk().sendRequest("GET", "/sapi/mediaclip", new RequestOptions().setSkipAuth(true));
        assertNull(f.server().call(0).header("rpctoken"));
    }

    @Test
    void failsClosedOnUnparseableUrl() {
        // A malformed absolute URL makes origin resolution fail; the guard must treat it as
        // cross-origin and never mint a token, and the transport failure is typed.
        CountingAuthenticator auth = new CountingAuthenticator();
        StubHttpClient stub = StubHttpClient.hanging();
        Sdk sdk = new Sdk("my-publication", auth, new SdkOptions().setHttpClient(stub));

        assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("PUT", "https://["));

        assertEquals(0, auth.calls.get());
        assertEquals(0, stub.sends());
    }

    @Test
    void defaultTimeoutAbortsASlowRequest() {
        MockServer slow = server(ok().delayed(400));
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(),
                new SdkOptions().setBaseUri(slow.baseUri()).setTimeout(Duration.ofMillis(50)));

        SapiConnectionException ex = assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

        assertTrue(ex.getMessage().contains("timed out after 50ms"), ex.getMessage());
    }

    @Test
    void zeroTimeoutDisablesItForThatRequest() {
        // Streaming uploads pass zero: their duration scales with file size.
        MockServer slow = server(ok().delayed(200));
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(),
                new SdkOptions().setBaseUri(slow.baseUri()).setTimeout(Duration.ofMillis(50)));

        SapiResponse response = sdk.sendRequest("PUT", slow.url(S3_PATH), new RequestOptions().setTimeout(Duration.ZERO));

        assertTrue(response.isOk());
    }

    @Test
    void zeroDefaultTimeoutDisablesTheDefault() {
        MockServer slow = server(ok().delayed(200));
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(),
                new SdkOptions().setBaseUri(slow.baseUri()).setTimeout(Duration.ZERO));
        assertTrue(sdk.sendRequest("GET", "/sapi/mediaclip").isOk());
    }

    @Test
    void perRequestTimeoutOverridesTheSdkDefault() {
        MockServer slow = server(ok().delayed(300));
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(), new SdkOptions().setBaseUri(slow.baseUri()));
        assertThrows(SapiConnectionException.class,
                () -> sdk.sendRequest("GET", "/sapi/mediaclip", new RequestOptions().setTimeout(Duration.ofMillis(50))));
    }

    @Test
    void uploadBodyHasContentLengthNotChunked() {
        Fixture f = create();
        MockServer s3 = server(ok());
        f.sdk().sendRequest("PUT", s3.url(S3_PATH), new RequestOptions()
                .setBody(HttpRequest.BodyPublishers.ofByteArray(new byte[10]))
                .setTimeout(Duration.ZERO));
        assertEquals("10", s3.call(0).header("Content-Length"));
        assertNull(s3.call(0).header("Transfer-Encoding"));
    }

    private static Sdk withStub(StubHttpClient stub, Duration timeout) {
        return new Sdk("my-publication", new EmptyAuthenticator(), new SdkOptions().setHttpClient(stub).setTimeout(timeout));
    }

    @Test
    void wrapsNetworkFailurePreservingCause() {
        ConnectException cause = new ConnectException("fetch failed");
        Sdk sdk = withStub(StubHttpClient.failing(cause), Duration.ofSeconds(30));

        SapiConnectionException ex = assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

        assertEquals(0, ex.getStatusCode());
        assertSame(cause, ex.getCause());
        assertTrue(ex.getMessage().contains("fetch failed"));
    }

    @Test
    void wrapsTimeout() {
        StubHttpClient stub = StubHttpClient.hanging();
        Sdk sdk = withStub(stub, Duration.ofMillis(50));

        SapiConnectionException ex = assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

        assertTrue(ex.getMessage().matches(".*timed out after 50ms.*"), ex.getMessage());
        assertInstanceOf(TimeoutException.class, ex.getCause());
        assertTrue(stub.lastFuture().isCancelled());
    }

    @Test
    void callerCancellationIsNotWrapped() throws InterruptedException {
        StubHttpClient stub = StubHttpClient.hanging();
        Sdk sdk = withStub(stub, Duration.ofSeconds(30));
        Thread caller = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                return;
            }
            caller.interrupt();
        });
        interrupter.start();

        try {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

            assertInstanceOf(CancellationException.class, ex);
            assertFalse(ex instanceof SapiRequestException);
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt flag must be restored");
            assertTrue(stub.lastFuture().isCancelled());
        } finally {
            Thread.interrupted();
            interrupter.join();
        }
    }

    @Test
    void wrapsArbitraryTransportException() {
        Sdk sdk = withStub(StubHttpClient.failing(new IOException("boom")), Duration.ofSeconds(30));

        SapiConnectionException ex = assertThrows(SapiConnectionException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip"));

        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void sapiExceptionsFromTheTransportAreNotWrapped() {
        SapiServerErrorException cause = new SapiServerErrorException("x", 500);
        Sdk sdk = withStub(StubHttpClient.failing(cause), Duration.ofSeconds(30));
        assertSame(cause, assertThrows(SapiServerErrorException.class, () -> sdk.sendRequest("GET", "/sapi/mediaclip")));
    }
}
