package com.bluebillywig.sapi.tools;

// Smoke run of the SDK against a real publication. Read-only unless the key carries write roles.
//
//   SAPI_AUTH_KEY=<tokenId>-<secret> SAPI_BASE_URL=https://testsuite.acc.bbvms.com \
//       java -cp <classpath> com.bluebillywig.sapi.tools.AccSmoke
//
// This is a developer tool, not a test: it lives under src/test/java so it is compiled with the
// tests but never ends up in the published jar, and its name does not match Surefire's patterns.
// Build the classpath with:  mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=cp.txt
// and run with:  java -cp target/classes:target/test-classes:$(cat cp.txt) <this class>
//
// Set SMOKE_UPLOAD_FILE=/path/to/video.mp4 to upload a real media file instead of the bundled
// 179-byte PNG: a file larger than one part exercises the multi-part path, progress polling and
// the wait for SAPI to finish processing the clip.
//
// SAPI_AUTH_KEY follows the bb-sapi CLI convention; SAPI_BASE_URL defaults to the publication's
// production host, so ALWAYS set it when targeting ACC.

import com.bluebillywig.sapi.SapiResponse;
import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.SdkOptions;
import com.bluebillywig.sapi.authentication.RpcTokenAuthenticator;
import com.bluebillywig.sapi.exceptions.SapiClientErrorException;
import com.bluebillywig.sapi.exceptions.SapiRequestException;
import com.bluebillywig.sapi.search.FilterOperator;
import com.bluebillywig.sapi.search.FilterSet;
import com.bluebillywig.sapi.types.MediaClipProps;
import com.bluebillywig.sapi.types.UploadData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Smoke run of the SDK against a real publication. See the comments at the top of this file. */
public final class AccSmoke {
    private AccSmoke() {
    }

    private static String env(String name) {
        return System.getenv(name);
    }

    private static void step(String name) {
        System.out.println("\n== " + name);
    }

    private static ObjectNode obj(SapiResponse r) {
        r.assertOk();
        return (ObjectNode) r.json();
    }

    /** Text of a node like .NET's JsonNode.ToString(): the raw string, JSON for anything else, empty when absent. */
    private static String txt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private static String cut(String s, int max) {
        return s.substring(0, Math.min(max, s.length()));
    }

    private static Path bundledSmokePng() throws IOException {
        Path target = Files.createTempFile("smoke-", ".png");
        target.toFile().deleteOnExit();
        try (InputStream in = AccSmoke.class.getResourceAsStream("/smoke.png")) {
            if (in == null) {
                throw new IOException("smoke.png is not on the classpath");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static HttpResponse<byte[]> httpGet(HttpClient http, String url) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Runs the smoke sequence.
     *
     * @param args unused; configuration is read from the environment
     * @throws Exception on setup failures
     */
    public static void main(String[] args) throws Exception {
        String key = env("SAPI_AUTH_KEY");
        if (key == null) {
            throw new IllegalStateException("Set SAPI_AUTH_KEY=<tokenId>-<secret>");
        }
        String publication = env("SAPI_PUBLICATION") != null ? env("SAPI_PUBLICATION") : "testsuite";
        String baseUrl = env("SAPI_BASE_URL");
        int dash = key.indexOf('-');
        int tokenId = Integer.parseInt(key.substring(0, dash));
        String secret = key.substring(dash + 1);
        Path uploadPath = env("SMOKE_UPLOAD_FILE") != null ? Path.of(env("SMOKE_UPLOAD_FILE")) : bundledSmokePng();

        Sdk sdk = new Sdk(publication, new RpcTokenAuthenticator(tokenId, secret), new SdkOptions().setBaseUri(baseUrl));
        System.out.println("BaseUri " + sdk.getBaseUri() + ", token id " + tokenId);

        Long createdId = null;
        int failures = 0;
        try {
            step("publication data");
            ObjectNode pub = sdk.getPublicationData();
            System.out.println("name=" + txt(pub.get("name")) + " defaultMediaAssetPath=" + txt(pub.get("defaultMediaAssetPath")));

            step("list 3 clips");
            ObjectNode list = obj(sdk.mediaClip().list(3));
            System.out.println("numfound=" + txt(list.get("numfound")) + " count=" + txt(list.get("count")));
            JsonNode first = list.get("items").get(0);
            long firstId = Long.parseLong(txt(first.get("id")));
            System.out.println("first id=" + firstId + " title=" + txt(first.get("title")) + " status=" + txt(first.get("status")));

            step("get first clip (lang=en, no jobs)");
            ObjectNode got = obj(sdk.mediaClip().get(firstId, "en", false));
            System.out.println("id=" + txt(got.get("id")) + " title=" + txt(got.get("title")) + " src=" + txt(got.get("src")));

            step("search: status is published AND title contains 'a'");
            FilterSet fs = FilterSet.create().where("status", FilterOperator.IS, "published").where("title", FilterOperator.CONTAINS, "a");
            ObjectNode search = obj(sdk.mediaClip().search(fs, 2));
            System.out.println("filterset=" + fs + " -> numfound=" + txt(search.get("numfound")) + " count=" + txt(search.get("count")));

            step("search: hasInteractivity is true (bool normalised) + isEmpty placeholder");
            ObjectNode s2 = obj(sdk.mediaClip().search(FilterSet.create().where("hasInteractivity", FilterOperator.IS, true), 1));
            ObjectNode s3 = obj(sdk.mediaClip().search(FilterSet.create().where("author", FilterOperator.IS_EMPTY), 1));
            ObjectNode s4 = obj(sdk.mediaClip().search(FilterSet.create().where("author", FilterOperator.IS_NOT_EMPTY), 1));
            System.out.println("hasInteractivity=true numfound=" + txt(s2.get("numfound")) + "; author isEmpty=" + txt(s3.get("numfound"))
                    + " isNotEmpty=" + txt(s4.get("numfound")) + " (sum vs total " + txt(list.get("numfound")) + ")");

            step("search: raw fq escape hatch fq[0]=status:published");
            ObjectNode s5 = obj(sdk.mediaClip().search(FilterSet.create(), 1, 0, "createddate desc", "*", List.of("status:published")));
            ObjectNode published = obj(sdk.mediaClip().search(FilterSet.create().where("status", FilterOperator.IS, "published"), 1));
            System.out.println("fq numfound=" + txt(s5.get("numfound")) + " (filterset published numfound=" + txt(published.get("numfound")) + ")");

            step("poster URL fetch");
            String poster = sdk.thumbnail().getMediaClipPosterPath(firstId, 320, 180);
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<byte[]> pr = httpGet(http, poster);
            System.out.println(poster + " -> " + pr.statusCode() + " " + pr.headers().firstValue("Content-Type").orElse("")
                    + " " + pr.body().length + " bytes");

            step("source path (absolute)");
            try {
                System.out.println(sdk.mediaClip().getSourcePath(firstId));
            } catch (IllegalStateException e) {
                System.out.println("no src: " + e.getMessage());
            }

            step("404 -> SapiClientErrorException");
            try {
                sdk.mediaClip().get(999999999).assertOk();
                System.out.println("UNEXPECTED: no throw");
                failures++;
            } catch (SapiClientErrorException e) {
                System.out.println("ok: " + e.getStatusCode() + " " + e.getMessage());
            }

            step("create clip");
            ObjectNode created = obj(sdk.mediaClip().create(new MediaClipProps().setTitle("java-sdk smoke " + Instant.now()).setStatus("draft")));
            createdId = Long.parseLong(txt(created.get("id")));
            System.out.println("created id=" + createdId + " title=" + txt(created.get("title")));

            step("update clip");
            ObjectNode updated = obj(sdk.mediaClip().update(createdId, new MediaClipProps().setDescription("updated by bb-sapi-java-sdk smoke run"), false, null));
            System.out.println("description=" + txt(updated.get("description")));

            step("initialize + execute upload (" + uploadPath.getFileName() + ", " + Files.size(uploadPath) + " bytes)");
            SapiResponse init = sdk.mediaClip().initializeUpload(uploadPath.toString(), createdId);
            System.out.println("init status=" + init.getStatusCode() + " body=" + cut(init.getBody(), 300));
            init.assertOk();
            UploadData upload = init.json(UploadData.class);
            System.out.println("chunks=" + upload.getChunks() + " urls=" + (upload.getPresignedUrls() == null ? null : upload.getPresignedUrls().size())
                    + " chunkSize=" + (upload.getPresignedUrls() == null ? null : upload.getPresignedUrls().get(0).getChunkSize())
                    + " key=" + upload.getKey() + " uploadId=" + (upload.getUploadId() == null || upload.getUploadId().isEmpty() ? "(none)" : "set")
                    + " listPartsUrl=" + (upload.getListPartsUrl() == null ? "null" : "set"));
            long started = System.nanoTime();
            boolean ok = sdk.mediaClip().executeUpload(uploadPath.toString(), upload);
            System.out.printf("executeUpload -> %s in %.1fs%n", ok, (System.nanoTime() - started) / 1e9);
            if (upload.getListPartsUrl() != null && upload.getHeadObjectUrl() != null) {
                Iterator<Double> progress = sdk.mediaClip().uploadProgress(
                        upload.getListPartsUrl(), upload.getHeadObjectUrl(), upload.getChunks(), 500, 20);
                while (progress.hasNext()) {
                    System.out.println("progress " + progress.next() + "%");
                }
            }

            step("wait for SAPI to process the clip (up to 3 minutes)");
            Instant deadline = Instant.now().plus(Duration.ofMinutes(3));
            ObjectNode after;
            do {
                Thread.sleep(5000);
                after = obj(sdk.mediaClip().get(createdId));
                JsonNode jobs = after.get("jobs");
                String jobSummary = "n/a";
                if (jobs != null && jobs.isArray()) {
                    List<String> parts = new ArrayList<>();
                    for (JsonNode job : jobs) {
                        parts.add(txt(job.get("type")) + ":" + txt(job.get("status")));
                    }
                    jobSummary = String.join(",", parts);
                }
                System.out.println("  src=" + txt(after.get("src")) + " originalfilename=" + txt(after.get("originalfilename"))
                        + " length=" + txt(after.get("length")) + " status=" + txt(after.get("status")) + " jobs=[" + jobSummary + "]");
                if (!txt(after.get("src")).isEmpty() && !txt(after.get("length")).isEmpty()) {
                    break;
                }
            } while (Instant.now().isBefore(deadline));
            if (txt(after.get("src")).isEmpty()) {
                System.out.println("clip not processed within the wait window");
                failures++;
            } else {
                System.out.println("source path: " + sdk.mediaClip().getSourcePath(createdId));
                HttpClient http2 = HttpClient.newHttpClient();
                String token = new RpcTokenAuthenticator(tokenId, secret).authenticate().get("rpctoken");
                HttpResponse<byte[]> posterResp = httpGet(http2, sdk.thumbnail().getMediaClipPosterPath(createdId, 320, 180, token));
                System.out.println("poster of new clip -> " + posterResp.statusCode() + " " + posterResp.headers().firstValue("Content-Type").orElse(""));
            }
        } catch (Exception e) {
            failures++;
            System.out.println("FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e instanceof SapiRequestException sre) {
                System.out.println(sre.getResponseBody());
            }
        } finally {
            if (createdId != null) {
                step("delete created clip (purge)");
                SapiResponse del = sdk.mediaClip().delete(createdId, true);
                System.out.println("delete -> " + del.getStatusCode() + " " + cut(del.getBody(), 200));
                SapiResponse gone = sdk.mediaClip().get(createdId);
                System.out.println("get after delete -> " + gone.getStatusCode());
            }
        }
        System.out.println("\nfailures=" + failures);
        System.exit(failures);
    }
}
