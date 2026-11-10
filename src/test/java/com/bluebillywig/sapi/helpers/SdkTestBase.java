package com.bluebillywig.sapi.helpers;

import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.SdkOptions;
import com.bluebillywig.sapi.authentication.EmptyAuthenticator;
import com.bluebillywig.sapi.helpers.MockServer.MockResponse;
import com.bluebillywig.sapi.util.QueryParams;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

/**
 * Base for tests that talk to {@link MockServer}s: the counterpart of the .NET {@code TestSdk}
 * helper. Servers started through it are stopped after each test.
 */
public abstract class SdkTestBase {
    /** An SDK wired to a mock SAPI server. */
    public record Fixture(Sdk sdk, MockServer server) {
    }

    @TempDir
    protected Path tempDir;

    private final List<MockServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(MockServer::close);
        servers.clear();
    }

    /** Starts a mock server (also usable as the "other origin", e.g. S3). */
    protected MockServer server(MockResponse... responses) {
        MockServer server = new MockServer(responses);
        servers.add(server);
        return server;
    }

    /** An SDK with no authentication, pointed at a mock server serving {@code responses}. */
    protected Fixture create(MockResponse... responses) {
        MockServer server = server(responses);
        Sdk sdk = new Sdk("my-publication", new EmptyAuthenticator(), new SdkOptions().setBaseUri(server.baseUri()));
        return new Fixture(sdk, server);
    }

    /** An SDK using RPC token authentication (id 1, secret "secret"), pointed at a mock server. */
    protected Fixture createWithRpcToken(MockResponse... responses) {
        MockServer server = server(responses);
        Sdk sdk = Sdk.withRpcTokenAuthentication("my-publication", 1, "secret", new SdkOptions().setBaseUri(server.baseUri()));
        return new Fixture(sdk, server);
    }

    /** An SDK on the default base URI, for tests that never touch the network. */
    protected static Sdk offlineSdk() {
        return new Sdk("my-publication", new EmptyAuthenticator());
    }

    protected static String query(String url, String name) {
        return QueryParams.get(url, name);
    }

    /** @return the raw (still percent-encoded) path of a URL */
    protected static String pathOf(String url) {
        return URI.create(url).getRawPath();
    }

    /** Writes a file of {@code size} bytes filled with {@code fill}. */
    protected Path tempFile(int size, int fill) {
        Path path = tempDir.resolve("bb-sdk-test-" + java.util.UUID.randomUUID() + ".bin");
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) fill);
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return path;
    }
}
