package com.bluebillywig.sapi.entities;

import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.ok;
import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.status;
import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.withHeader;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.SapiResponse;
import com.bluebillywig.sapi.contracts.Creatable;
import com.bluebillywig.sapi.contracts.Deletable;
import com.bluebillywig.sapi.contracts.Gettable;
import com.bluebillywig.sapi.contracts.Updatable;
import com.bluebillywig.sapi.exceptions.SapiConnectionException;
import com.bluebillywig.sapi.exceptions.SapiServerErrorException;
import com.bluebillywig.sapi.helpers.MockServer;
import com.bluebillywig.sapi.helpers.SdkTestBase;
import com.bluebillywig.sapi.types.MediaClipProps;
import com.bluebillywig.sapi.types.PresignedUrl;
import com.bluebillywig.sapi.types.S3Part;
import com.bluebillywig.sapi.types.UploadData;
import com.bluebillywig.sapi.util.SapiJson;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MediaClipTest extends SdkTestBase {
    @Test
    void listWithCorrectParams() {
        Fixture f = create(ok());
        f.sdk().mediaClip().list(15, 1, "createddate asc");
        String url = f.server().call(0).url();
        assertEquals("15", query(url, "limit"));
        assertEquals("1", query(url, "offset"));
        assertEquals("createddate asc", query(url, "sort"));
        assertEquals("/sapi/mediaclip", pathOf(url));
        assertEquals("GET", f.server().call(0).method());
    }

    @Test
    void delete() {
        Fixture f = create(ok());
        f.sdk().mediaClip().delete(1);
        assertEquals(f.server().url("/sapi/mediaclip/1"), f.server().call(0).url());
        assertEquals("DELETE", f.server().call(0).method());
    }

    @Test
    void deleteWithPurge() {
        Fixture f = create(ok());
        f.sdk().mediaClip().delete(1, true);
        assertEquals("true", query(f.server().call(0).url(), "purge"));
        assertEquals("/sapi/mediaclip/1", pathOf(f.server().call(0).url()));
    }

    @Test
    void initializeUploadThrowsForMissingFile() {
        Fixture f = create();
        final String path = "./path/to/a/non/existing/mediaclip/file";
        UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> f.sdk().mediaClip().initializeUpload(path));
        assertTrue(ex.getMessage().contains("File " + path + " is not a file or does not exist."));
        assertInstanceOf(FileNotFoundException.class, ex.getCause());
        assertTrue(f.server().calls().isEmpty());
    }

    @Test
    void initializeUploadWithCorrectParams() {
        Fixture f = create(ok());
        Path path = tempFile(10, 0x41);

        f.sdk().mediaClip().initializeUpload(path.toString());

        String url = f.server().call(0).url();
        assertEquals("/sapi/mediaclip/0/upload", pathOf(url));
        assertEquals(path.getFileName().toString(), query(url, "filename"));
        assertEquals("10", query(url, "filesize"));
        assertEquals("application/octet-stream", query(url, "contenttype"));
        assertNull(query(url, "clipid"));
        assertEquals("GET", f.server().call(0).method());
    }

    @Test
    void initializeUploadWithMediaClipId() {
        Fixture f = create(ok());
        Path path = tempFile(10, 0x41);
        f.sdk().mediaClip().initializeUpload(path.toString(), 1);
        assertEquals("1", query(f.server().call(0).url(), "clipid"));
    }

    @ParameterizedTest
    @CsvSource({
        "clip.mp4,video/mp4",
        "CLIP.MOV,video/quicktime",
        "a.jpg,image/jpeg",
        "a.unknown,application/octet-stream"})
    void mimeTypeByExtension(String file, String expected) {
        assertEquals(expected, MediaClip.getMimeType(file));
    }

    @Test
    void abortUpload() {
        Fixture f = create(ok());
        f.sdk().mediaClip().abortUpload("/prefix/my-video.mp4", "12345");
        String url = f.server().call(0).url();
        assertEquals("/prefix/my-video.mp4", query(url, "s3filekey"));
        assertEquals("12345", query(url, "s3uploadid"));
        assertEquals("/sapi/mediaclip/0/abortUpload", pathOf(url));
        assertEquals("PUT", f.server().call(0).method());
    }

    @Test
    void completeUpload() {
        Fixture f = create(ok());
        List<S3Part> parts = List.of(new S3Part("12345", "1"), new S3Part("12346", "2"), new S3Part("12347", "3"));

        f.sdk().mediaClip().completeUpload("/prefix/my-video.mp4", "12345", parts);

        assertEquals(f.server().url("/sapi/mediaclip/0/completeUpload"), f.server().call(0).url());
        assertEquals("PUT", f.server().call(0).method());
        JsonNode expected = SapiJson.readTree("{\"s3FileKey\":\"/prefix/my-video.mp4\",\"s3UploadId\":\"12345\",\"s3Parts\":[{\"ETag\":\"12345\",\"PartNumber\":\"1\"},{\"ETag\":\"12346\",\"PartNumber\":\"2\"},{\"ETag\":\"12347\",\"PartNumber\":\"3\"}]}");
        assertEquals(expected, SapiJson.readTree(f.server().call(0).bodyText()), f.server().call(0).bodyText());
    }

    @Test
    void get() {
        Fixture f = create(ok());
        f.sdk().mediaClip().get(1);
        String url = f.server().call(0).url();
        assertEquals("true", query(url, "includejobs"));
        assertNull(query(url, "lang"));
        assertEquals("/sapi/mediaclip/1", pathOf(url));
        assertEquals("GET", f.server().call(0).method());
    }

    @Test
    void getWithLangAndNoJobs() {
        Fixture f = create(ok());
        f.sdk().mediaClip().get(1, "en", false);
        String url = f.server().call(0).url();
        assertEquals("false", query(url, "includejobs"));
        assertEquals("en", query(url, "lang"));
    }

    @Test
    void createAndUpdate() {
        Fixture f = create(ok(), ok(), ok(), ok());
        MediaClipProps props = new MediaClipProps().setTitle("My Mediaclip");

        f.sdk().mediaClip().create(props);
        f.sdk().mediaClip().create(props, true, "en");
        f.sdk().mediaClip().update(1, props);
        f.sdk().mediaClip().update(1, props, true, "en");

        List<MockServer.RecordedCall> calls = f.server().calls();
        assertEquals("false", query(calls.get(0).url(), "softsave"));
        assertEquals("/sapi/mediaclip", pathOf(calls.get(0).url()));
        assertEquals("PUT", calls.get(0).method());
        assertEquals("{\"title\":\"My Mediaclip\"}", calls.get(0).bodyText());
        assertEquals("true", query(calls.get(1).url(), "softsave"));
        assertEquals("en", query(calls.get(1).url(), "lang"));
        assertEquals("/sapi/mediaclip/1", pathOf(calls.get(2).url()));
        assertEquals("false", query(calls.get(2).url(), "softsave"));
        assertEquals("{\"title\":\"My Mediaclip\"}", calls.get(2).bodyText());
        assertEquals("true", query(calls.get(3).url(), "softsave"));
        assertEquals("en", query(calls.get(3).url(), "lang"));
    }

    @Test
    void contractInterfacesRouteToDefaults() {
        Fixture f = create(ok(), ok(), ok(), ok());
        Gettable gettable = f.sdk().mediaClip();
        Creatable<MediaClipProps> creatable = f.sdk().mediaClip();
        Updatable<MediaClipProps> updatable = f.sdk().mediaClip();
        Deletable deletable = f.sdk().mediaClip();

        gettable.get("1");
        creatable.create(new MediaClipProps().setTitle("t"));
        updatable.update("1", new MediaClipProps().setTitle("t"));
        deletable.delete("1");

        List<MockServer.RecordedCall> calls = f.server().calls();
        assertEquals("true", query(calls.get(0).url(), "includejobs"));
        assertEquals("false", query(calls.get(1).url(), "softsave"));
        assertEquals("false", query(calls.get(2).url(), "softsave"));
        assertNull(query(calls.get(3).url(), "purge"));
    }

    // -- uploads: the presigned URLs live on another origin (the S3 stand-in) -----------------

    private static UploadData threePartUpload(MockServer s3) {
        return new UploadData()
                .setKey("/prefix/blank.mp4")
                .setUploadId("12345")
                .setChunks(3)
                .setPresignedUrls(List.of(
                        new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url/1?partNumber=1")).setChunkSize(10).setOffset(0),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url/2?partNumber=2")).setChunkSize(10).setOffset(10),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url/3?partNumber=3")).setChunkSize(10).setOffset(20)));
    }

    @Test
    void executeSingleChunkUpload() {
        Fixture f = createWithRpcToken();
        MockServer s3 = server(ok());
        Path path = tempFile(10, 0x41);

        boolean result = f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setChunks(1)
                .setPresignedUrls(List.of(new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url")).setChunkSize(10))));

        assertTrue(result);
        assertEquals(1, s3.calls().size());
        MockServer.RecordedCall call = s3.call(0);
        assertEquals("PUT", call.method());
        assertEquals(s3.url("/presigned-url"), call.url());
        // A sized body so S3 gets a Content-Length and no chunked Transfer-Encoding.
        assertEquals("10", call.header("Content-Length"));
        assertNull(call.header("Transfer-Encoding"));
        assertEquals("A".repeat(10), call.bodyText());
        assertNull(call.header("rpctoken"));
        assertTrue(f.server().calls().isEmpty());
    }

    @Test
    void chunkSizeDefaultsToFileSize() {
        Fixture f = create();
        MockServer s3 = server(ok());
        Path path = tempFile(10, 0x41);

        boolean result = f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setChunks(1)
                .setPresignedUrls(List.of(new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url")))));

        assertTrue(result);
        assertEquals(10, s3.call(0).body().length);
    }

    @Test
    void executeMultiChunkUpload() {
        Fixture f = createWithRpcToken(ok());
        MockServer s3 = server(
                withHeader("ETag", "\"some-etag-1\""),
                withHeader("ETag", "\"some-etag-2\""),
                withHeader("ETag", "\"some-etag-3\""));
        Path path = tempFile(30, 0x42);

        boolean result = f.sdk().mediaClip().executeUpload(path.toString(), threePartUpload(s3));

        assertTrue(result);
        assertEquals(3, s3.calls().size());
        assertEquals(1, f.server().calls().size());
        for (int i = 0; i < 3; i++) {
            assertEquals("PUT", s3.call(i).method());
            assertEquals(10, s3.call(i).body().length);
            assertEquals("10", s3.call(i).header("Content-Length"));
            assertNull(s3.call(i).header("Transfer-Encoding"));
            assertNull(s3.call(i).header("rpctoken"));
        }

        MockServer.RecordedCall complete = f.server().call(0);
        assertEquals("PUT", complete.method());
        assertEquals("/sapi/mediaclip/0/completeUpload", pathOf(complete.url()));
        assertTrue(complete.header("rpctoken").startsWith("1-"));
        JsonNode body = SapiJson.readTree(complete.bodyText());
        assertEquals("/prefix/blank.mp4", body.get("s3FileKey").asText());
        assertEquals("12345", body.get("s3UploadId").asText());
        JsonNode parts = body.get("s3Parts");
        assertEquals(3, parts.size());
        List<String> numbers = new ArrayList<>();
        parts.forEach(p -> numbers.add(p.get("PartNumber").asText()));
        numbers.sort(null);
        assertEquals(List.of("1", "2", "3"), numbers);
        parts.forEach(p -> assertTrue(p.get("ETag").asText().matches("^some-etag-\\d$")));
    }

    @Test
    void handlesMissingETagAndPartNumber() {
        Fixture f = create(ok());
        MockServer s3 = server(ok(), ok());
        Path path = tempFile(20, 0x44);

        boolean result = f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setKey("/prefix/blank.mp4")
                .setUploadId("12345")
                .setChunks(2)
                .setPresignedUrls(List.of(
                        new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url/1")).setChunkSize(10).setOffset(0),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/presigned-url/2?partNumber=2")).setChunkSize(10).setOffset(10))));

        assertTrue(result);
        JsonNode parts = SapiJson.readTree(f.server().call(0).bodyText()).get("s3Parts");
        assertEquals("", parts.get(0).get("ETag").asText());
        assertEquals("", parts.get(1).get("ETag").asText());
        List<String> numbers = new ArrayList<>();
        parts.forEach(p -> numbers.add(p.get("PartNumber").asText()));
        assertTrue(numbers.contains(""));
        assertTrue(numbers.contains("2"));
    }

    @Test
    void abortsUploadOnChunkFailure() {
        Fixture f = create(ok());
        MockServer s3 = server(
                withHeader("ETag", "\"some-etag-1\""),
                withHeader("ETag", "\"some-etag-2\""),
                status(500));
        Path path = tempFile(30, 0x43);

        assertThrows(SapiServerErrorException.class, () -> f.sdk().mediaClip().executeUpload(path.toString(), threePartUpload(s3)));

        assertEquals(3, s3.calls().size());
        assertEquals(1, f.server().calls().size());
        MockServer.RecordedCall abort = f.server().call(0);
        assertEquals("PUT", abort.method());
        assertEquals("/sapi/mediaclip/0/abortUpload", pathOf(abort.url()));
        assertEquals("/prefix/blank.mp4", query(abort.url(), "s3filekey"));
        assertEquals("12345", query(abort.url(), "s3uploadid"));
    }

    @Test
    void stillThrowsOriginalErrorWhenAbortFails() {
        Fixture f = create(status(500));
        MockServer s3 = server(
                withHeader("ETag", "\"some-etag-1\""),
                withHeader("ETag", "\"some-etag-2\""),
                status(500));
        Path path = tempFile(30, 0x43);

        assertThrows(SapiServerErrorException.class, () -> f.sdk().mediaClip().executeUpload(path.toString(), threePartUpload(s3)));
        assertEquals(1, f.server().calls().size());
    }

    @Test
    void executeUploadThrowsForInvalidPath() {
        Fixture f = create();
        MockServer s3 = server();
        UncheckedIOException ex = assertThrows(UncheckedIOException.class, () -> f.sdk().mediaClip().executeUpload("./nonexistent", new UploadData()
                .setChunks(1)
                .setPresignedUrls(List.of(new PresignedUrl().setPresignedUrlValue(s3.url("/url"))))));
        assertTrue(ex.getMessage().contains("is not a file or does not exist"));
    }

    @Test
    void executeUploadThrowsForMissingChunksOrUrls() {
        Fixture f = create();
        Path path = tempFile(10, 0x41);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()));
        assertTrue(ex.getMessage().contains("uploadData must contain 'chunks' and 'presignedUrls' keys."));
    }

    @Test
    void executeUploadThrowsForMultiPartMissingKeyOrUploadId() {
        Fixture f = create();
        MockServer s3 = server();
        Path path = tempFile(10, 0x41);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setChunks(3)
                .setPresignedUrls(List.of(
                        new PresignedUrl().setPresignedUrlValue(s3.url("/url/1")),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/url/2")),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/url/3"))))));
        assertTrue(ex.getMessage().contains("uploadData for multi-part uploads must contain 'key' and 'uploadId' keys."));
        assertTrue(s3.calls().isEmpty());
    }

    @Test
    void uploadDataDeserializesFromSapiJson() {
        Fixture f = create(ok("{\"chunks\":2,\"key\":\"k\",\"uploadId\":\"u\",\"listPartsUrl\":\"https://s3/l\",\"headObjectUrl\":\"https://s3/h\",\"presignedUrls\":[{\"presignedUrl\":\"https://s3/1\",\"offset\":0,\"chunkSize\":5},{\"presignedUrl\":\"https://s3/2\",\"offset\":5,\"chunkSize\":5}]}"));
        SapiResponse response = f.sdk().sendRequest("GET", "/sapi/mediaclip/0/upload");

        UploadData data = response.json(UploadData.class);

        assertEquals(2, data.getChunks());
        assertEquals("k", data.getKey());
        assertEquals("https://s3/l", data.getListPartsUrl());
        assertEquals(2, data.getPresignedUrls().size());
        assertEquals(5L, data.getPresignedUrls().get(1).getOffset());
        assertEquals("https://s3/2", data.getPresignedUrls().get(1).getPresignedUrlValue());
    }

    // -- progress ---------------------------------------------------------------------------

    @Test
    void progress0WhenNotStarted() {
        Fixture f = create();
        MockServer s3 = server(status(404), status(404));
        assertEquals(0, f.sdk().mediaClip().getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
    }

    @Test
    void progress100WhenCompleted() {
        Fixture f = create();
        MockServer s3 = server(status(404), ok());
        assertEquals(100, f.sdk().mediaClip().getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
        assertEquals("HEAD", s3.call(1).method());
        assertEquals("/head-object", pathOf(s3.call(1).url()));
    }

    @Test
    void progressForSinglePartObject() {
        Fixture f = create();
        MockServer s3 = server(ok("{\"Part\":{\"PartNumber\":1}}"));
        assertEquals(20, f.sdk().mediaClip().getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
    }

    @Test
    void progressForMultipleParts() {
        Fixture f = create();
        MockServer s3 = server(ok("{\"Part\":[{},{},{}]}"), ok("{\"Part\":[{},{},{},{},{}]}"), ok("{}"));
        MediaClip clip = f.sdk().mediaClip();
        assertEquals(60, clip.getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
        assertEquals(100, clip.getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
        assertEquals(0, clip.getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
    }

    @Test
    void progressThrowsOnUnexpectedStatus() {
        Fixture f = create();
        MockServer s3 = server(status(500));
        assertThrows(SapiServerErrorException.class,
                () -> f.sdk().mediaClip().getUploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5));
    }

    @Test
    void progressStreamYieldsUntil100() {
        Fixture f = create();
        MockServer s3 = server(
                status(404), status(404),
                ok("{\"Part\":[{}]}"), ok("{\"Part\":[{},{}]}"), ok("{\"Part\":[{},{},{}]}"), ok("{\"Part\":[{},{},{},{}]}"),
                status(404), ok());

        List<Double> progress = new ArrayList<>();
        Iterator<Double> it = f.sdk().mediaClip().uploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5, 10, 0);
        while (it.hasNext()) {
            progress.add(it.next());
        }

        assertEquals(List.of(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), progress);
    }

    @Test
    void progressStreamThrowsWhenMaxIterationsExceeded() {
        Fixture f = create();
        MockServer s3 = server(status(404), status(404), status(404), status(404));
        Iterator<Double> it = f.sdk().mediaClip().uploadProgress(s3.url("/list-part"), s3.url("/head-object"), 5, 10, 2);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            while (it.hasNext()) {
                it.next();
            }
        });
        assertEquals("Upload progress polling exceeded the maximum of 2 iterations.", ex.getMessage());
    }

    // -- source path ------------------------------------------------------------------------

    @Test
    void sourcePathThrowsWithoutSrc() {
        Fixture f = create(ok("{\"title\":\"No src here\"}"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> f.sdk().mediaClip().getSourcePath(1));
        assertEquals("MediaClip response does not contain a 'src' field.", ex.getMessage());
    }

    @Test
    void relativeSourcePath() {
        Fixture f = create(ok("{\"src\":\"/some/source/of/mediaclip.mp4\"}"));
        assertEquals("/some/source/of/mediaclip.mp4", f.sdk().mediaClip().getSourcePath(1, false));
    }

    @Test
    void absoluteSourcePath() {
        Fixture f = create(ok("{\"src\":\"/some/source/of/mediaclip.mp4\"}"), ok("{\"defaultMediaAssetPath\":\"https://my-cfn.bluebillywig.com\"}"));
        assertEquals("https://my-cfn.bluebillywig.com/some/source/of/mediaclip.mp4", f.sdk().mediaClip().getSourcePath(1, true));
    }

    @Test
    void absoluteSourcePathThrowsWithoutDefaultMediaAssetPath() {
        Fixture f = create(ok("{\"src\":\"/x.mp4\"}"), ok("{}"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> f.sdk().mediaClip().getSourcePath(1, true));
        assertEquals("Publication data missing 'defaultMediaAssetPath'.", ex.getMessage());
    }

    // -- beyond the .NET suite ---------------------------------------------------------------

    @Test
    void nullSrcOrNullDefaultMediaAssetPathCountsAsMissing() {
        Fixture f = create(ok("{\"src\":null}"));
        assertThrows(IllegalStateException.class, () -> f.sdk().mediaClip().getSourcePath(1, false));

        Fixture g = create(ok("{\"src\":\"/x.mp4\"}"), ok("{\"defaultMediaAssetPath\":null}"));
        assertThrows(IllegalStateException.class, () -> g.sdk().mediaClip().getSourcePath(1, true));
    }

    @Test
    void eachPartIsItsOwnSizedSliceOfTheFileSentInOrder() throws IOException {
        Fixture f = create(ok());
        MockServer s3 = server(ok(), ok(), ok());
        byte[] content = new byte[25];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        Path path = tempDir.resolve("slices.bin");
        Files.write(path, content);

        UploadData data = new UploadData()
                .setKey("k")
                .setUploadId("u")
                .setChunks(3)
                .setPresignedUrls(List.of(
                        new PresignedUrl().setPresignedUrlValue(s3.url("/p/1?partNumber=1")).setOffset(0).setChunkSize(10),
                        new PresignedUrl().setPresignedUrlValue(s3.url("/p/2?partNumber=2")).setOffset(10).setChunkSize(10),
                        // chunkSize beyond the end of the file is clipped to what is left
                        new PresignedUrl().setPresignedUrlValue(s3.url("/p/3?partNumber=3")).setOffset(20).setChunkSize(10)));

        assertTrue(f.sdk().mediaClip().executeUpload(path.toString(), data));

        assertArrayEquals(Arrays.copyOfRange(content, 0, 10), s3.call(0).body());
        assertArrayEquals(Arrays.copyOfRange(content, 10, 20), s3.call(1).body());
        assertArrayEquals(Arrays.copyOfRange(content, 20, 25), s3.call(2).body());
        assertEquals("5", s3.call(2).header("Content-Length"));
        assertEquals("/p/1?partNumber=1", s3.call(0).url().substring(s3.baseUri().length()));
    }

    @Test
    void sapiRequestsMadeByTheUploadFlowCarryTheTokenButS3RequestsNeverDo() {
        Fixture f = createWithRpcToken(ok(), ok());
        MockServer s3 = server(ok(), ok("{\"Part\":[{}]}"), ok());
        Path path = tempFile(20, 0x41);

        f.sdk().mediaClip().initializeUpload(path.toString());
        f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setChunks(1)
                .setPresignedUrls(List.of(new PresignedUrl().setPresignedUrlValue(s3.url("/p")))));
        double progress = f.sdk().mediaClip().getUploadProgress(s3.url("/list-part?X-Amz-Signature=abc"), s3.url("/head"), 4);

        assertEquals(25.0, progress);
        assertTrue(f.server().call(0).header("rpctoken").startsWith("1-"));
        for (MockServer.RecordedCall call : s3.calls()) {
            assertNull(call.header("rpctoken"), call.url());
        }
        assertEquals(2, s3.calls().size());
    }

    @Test
    void progressIteratorIsLazyAndPollsOncePerNext() {
        Fixture f = create();
        MockServer s3 = server(ok("{\"Part\":[{}]}"), ok("{}"));
        Iterator<Double> it = f.sdk().mediaClip().uploadProgress(s3.url("/l"), s3.url("/h"), 2, 1, 0);

        assertTrue(s3.calls().isEmpty());
        assertTrue(it.hasNext());
        assertTrue(s3.calls().isEmpty());
        assertEquals(50.0, it.next());
        assertEquals(1, s3.calls().size());
        assertTrue(it.hasNext());
    }

    @Test
    void connectionFailureDuringAPartPropagatesWithoutAbort() {
        // Same as the .NET flow: only a rejected response triggers the abort, a transport failure
        // (here: nothing listens on the presigned host) propagates as is.
        Fixture f = create(ok());
        MockServer gone = new MockServer();
        String url = gone.url("/p");
        gone.close();
        Path path = tempFile(20, 0x41);

        assertThrows(SapiConnectionException.class, () -> f.sdk().mediaClip().executeUpload(path.toString(), new UploadData()
                .setKey("k")
                .setUploadId("u")
                .setChunks(2)
                .setPresignedUrls(List.of(
                        new PresignedUrl().setPresignedUrlValue(url).setOffset(0).setChunkSize(10),
                        new PresignedUrl().setPresignedUrlValue(url).setOffset(10).setChunkSize(10)))));
        assertFalse(f.server().calls().stream().anyMatch(c -> c.url().contains("abortUpload")));
    }
}
