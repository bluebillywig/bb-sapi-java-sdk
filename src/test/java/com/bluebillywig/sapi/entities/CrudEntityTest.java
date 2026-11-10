package com.bluebillywig.sapi.entities;

import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.helpers.SdkTestBase;
import com.bluebillywig.sapi.types.ChannelConfig;
import com.bluebillywig.sapi.types.ChannelProps;
import com.bluebillywig.sapi.types.PlaylistProps;
import com.bluebillywig.sapi.types.PlayoutProps;
import com.bluebillywig.sapi.types.SubtitleProps;
import com.bluebillywig.sapi.util.SapiJson;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Ports of the .NET ChannelTests, PlaylistTests, PlayoutTests, SubtitleTests and ThumbnailTests. */
class CrudEntityTest {
    @Nested
    class ChannelTests extends SdkTestBase {
        @Test
        void list() {
            Fixture f = create(ok());
            f.sdk().channel().list(15, 1, "createddate asc");
            String url = f.server().call(0).url();
            assertEquals("15", query(url, "limit"));
            assertEquals("1", query(url, "offset"));
            assertEquals("createddate asc", query(url, "sort"));
            assertEquals("/sapi/channel", pathOf(url));
            assertEquals("GET", f.server().call(0).method());
        }

        @Test
        void get() {
            Fixture f = create(ok());
            f.sdk().channel().get(1);
            assertEquals(f.server().url("/sapi/channel/1"), f.server().call(0).url());
            assertEquals("GET", f.server().call(0).method());
        }

        @Test
        void createChannel() {
            Fixture f = create(ok());
            f.sdk().channel().create(new ChannelProps().setConfig(new ChannelConfig().setPlayIn("inline")));
            assertEquals(f.server().url("/sapi/channel"), f.server().call(0).url());
            assertEquals("PUT", f.server().call(0).method());
            assertEquals(SapiJson.readTree("{\"config\":{\"playIn\":\"inline\"}}"), SapiJson.readTree(f.server().call(0).bodyText()));
        }

        @Test
        void updateChannel() {
            Fixture f = create(ok());
            f.sdk().channel().update(1, new ChannelProps().setConfig(new ChannelConfig().setPlayIn("overlay")));
            assertEquals(f.server().url("/sapi/channel/1"), f.server().call(0).url());
            assertEquals("PUT", f.server().call(0).method());
            assertEquals(SapiJson.readTree("{\"config\":{\"playIn\":\"overlay\"}}"), SapiJson.readTree(f.server().call(0).bodyText()));
        }

        @Test
        void deleteChannel() {
            Fixture f = create(ok());
            f.sdk().channel().delete(1);
            assertEquals(f.server().url("/sapi/channel/1"), f.server().call(0).url());
            assertEquals("DELETE", f.server().call(0).method());
        }
    }

    @Nested
    class PlaylistTests extends SdkTestBase {
        @Test
        void list() {
            Fixture f = create(ok());
            f.sdk().playlist().list(10, 5, "createddate asc");
            String url = f.server().call(0).url();
            assertEquals("10", query(url, "limit"));
            assertEquals("5", query(url, "offset"));
            assertEquals("/sapi/playlist", pathOf(url));
        }

        @Test
        void getCreateUpdateDelete() {
            Fixture f = create(ok(), ok(), ok(), ok());
            f.sdk().playlist().get(1);
            f.sdk().playlist().create(new PlaylistProps().setTitle("My Playlist"));
            f.sdk().playlist().update(1, new PlaylistProps().setTitle("My Updated Playlist"));
            f.sdk().playlist().delete(1);

            assertEquals(f.server().url("/sapi/playlist/1"), f.server().call(0).url());
            assertEquals("GET", f.server().call(0).method());
            assertEquals(f.server().url("/sapi/playlist"), f.server().call(1).url());
            assertEquals("PUT", f.server().call(1).method());
            assertEquals("{\"title\":\"My Playlist\"}", f.server().call(1).bodyText());
            assertEquals(f.server().url("/sapi/playlist/1"), f.server().call(2).url());
            assertEquals("{\"title\":\"My Updated Playlist\"}", f.server().call(2).bodyText());
            assertEquals("DELETE", f.server().call(3).method());
        }

        @Test
        void mediaClipListAlias() {
            Fixture f = create(ok());
            f.sdk().mediaClipList().get(1);
            assertEquals(f.server().url("/sapi/playlist/1"), f.server().call(0).url());
        }

        @Test
        void stringIdIsEscaped() {
            Fixture f = create(ok());
            f.sdk().playlist().get("a b/c");
            assertEquals(f.server().url("/sapi/playlist/a%20b%2Fc"), f.server().call(0).url());
        }
    }

    @Nested
    class PlayoutTests extends SdkTestBase {
        @Test
        void listUsesSapiPlayout() {
            Fixture f = create(ok());
            f.sdk().playout().list(10, 5, "createddate asc");
            assertEquals("/sapi/playout", pathOf(f.server().call(0).url()));
            assertEquals("createddate asc", query(f.server().call(0).url(), "sort"));
        }

        @Test
        void getCreateUpdateDelete() {
            Fixture f = create(ok(), ok(), ok(), ok());
            f.sdk().playout().get(1);
            f.sdk().playout().create(new PlayoutProps().setName("My Playout"));
            f.sdk().playout().update(1, new PlayoutProps().setName("My Updated Playout"));
            f.sdk().playout().delete(1);

            assertEquals(f.server().url("/sapi/playout/1"), f.server().call(0).url());
            assertEquals("{\"name\":\"My Playout\"}", f.server().call(1).bodyText());
            assertEquals(f.server().url("/sapi/playout/1"), f.server().call(2).url());
            assertEquals("{\"name\":\"My Updated Playout\"}", f.server().call(2).bodyText());
            assertEquals("DELETE", f.server().call(3).method());
        }
    }

    @Nested
    class SubtitleTests extends SdkTestBase {
        @Test
        void list() {
            Fixture f = create(ok());
            f.sdk().subtitle().list(10, 5, "createddate asc");
            assertEquals("/sapi/subtitle", pathOf(f.server().call(0).url()));
        }

        @Test
        void getCreateUpdateDelete() {
            Fixture f = create(ok(), ok(), ok(), ok());
            f.sdk().subtitle().get(1);
            f.sdk().subtitle().create(new SubtitleProps().setMediaclipId(1).setIsocode("en"));
            f.sdk().subtitle().update(1, new SubtitleProps().setStatus("published"));
            f.sdk().subtitle().delete(1);

            assertEquals(f.server().url("/sapi/subtitle/1"), f.server().call(0).url());
            assertEquals(SapiJson.readTree("{\"mediaclipId\":1,\"isocode\":\"en\"}"), SapiJson.readTree(f.server().call(1).bodyText()));
            assertEquals("{\"status\":\"published\"}", f.server().call(2).bodyText());
            assertEquals("DELETE", f.server().call(3).method());
        }

        @Test
        void additionalPropertiesAreSentVerbatim() {
            Fixture f = create(ok());
            f.sdk().subtitle().create(new SubtitleProps().setStatus("draft").setAdditionalProperties(Map.of("custom", 42)));
            assertEquals(SapiJson.readTree("{\"status\":\"draft\",\"custom\":42}"), SapiJson.readTree(f.server().call(0).bodyText()));
        }

        // Beyond the .NET suite: props wire parity.

        @Test
        void additionalPropertiesComeBackWhenDeserialising() {
            SubtitleProps props = SapiJson.read("{\"status\":\"draft\",\"custom\":42,\"nested\":{\"a\":[1,2]}}", SubtitleProps.class);
            assertEquals("draft", props.getStatus());
            assertEquals(42, props.getAdditionalProperties().get("custom"));
            assertEquals("{\"status\":\"draft\",\"custom\":42,\"nested\":{\"a\":[1,2]}}", SapiJson.write(props));
        }

        @Test
        void unsetPropertiesAreOmittedAndEmptyPropsAreAnEmptyObject() {
            assertEquals("{}", SapiJson.write(new SubtitleProps()));
            assertEquals("{}", SapiJson.write(new PlaylistProps()));
            // Properties go out in declaration order, like System.Text.Json does.
            assertEquals("{\"shuffleOrder\":false,\"limit\":0}", SapiJson.write(new PlaylistProps().setLimit(0).setShuffleOrder(false)));
        }

        @Test
        void unknownPropertiesAreIgnoredAndNamesAreCaseInsensitiveWhenReading() {
            PlayoutProps props = SapiJson.read("{\"NAME\":\"x\",\"autoplay\":true,\"logoId\":7}", PlayoutProps.class);
            assertEquals("x", props.getName());
            assertEquals(true, props.getAutoPlay());
            assertEquals(7L, props.getLogoId());
        }
    }

    @Nested
    class ThumbnailTests extends SdkTestBase {
        @Test
        void absoluteImagePathWithDimensions() {
            Thumbnail thumbnail = offlineSdk().thumbnail();
            assertEquals("https://my-publication.bbvms.com/image/0/200/some/path/to/an/image", thumbnail.getAbsoluteImagePath("/some/path/to/an/image", 0, 200));
            assertEquals("https://my-publication.bbvms.com/image/300/0/some/path/to/an/image", thumbnail.getAbsoluteImagePath("some/path/to/an/image", 300, 0));
        }

        @Test
        void throwsForNegativeDimensions() {
            Thumbnail thumbnail = offlineSdk().thumbnail();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> thumbnail.getAbsoluteImagePath("some/path", -1, 0));
            assertTrue(ex.getMessage().contains("Given width is lower than 0."));
            ex = assertThrows(IllegalArgumentException.class, () -> thumbnail.getAbsoluteImagePath("some/path", 0, -1));
            assertTrue(ex.getMessage().contains("Given height is lower than 0."));
        }

        @Test
        void posterUrlFromOvpThumbnailRoute() {
            assertEquals("https://my-publication.bbvms.com/mediaclip/1234/spthumbnail/320/180.webp", offlineSdk().thumbnail().getMediaClipPosterPath(1234, 320, 180));
        }

        @Test
        void posterUrlLetsServiceChooseByDefault() {
            assertEquals("https://my-publication.bbvms.com/mediaclip/1234/spthumbnail/default/default.webp", offlineSdk().thumbnail().getMediaClipPosterPath(1234));
        }

        @Test
        void posterUrlCarriesRpcToken() {
            Thumbnail thumbnail = offlineSdk().thumbnail();
            assertEquals(
                    "https://my-publication.bbvms.com/mediaclip/1234/spthumbnail/default/default.webp?useSession=true&rpctoken=12-345678",
                    thumbnail.getMediaClipPosterPath(1234, null, null, "12-345678"));
            assertTrue(!thumbnail.getMediaClipPosterPath(1234, null, null, "").contains("?"));
        }

        @ParameterizedTest
        @CsvSource({"-1,default", "0,0", "99999,99999", "100000,default"})
        void posterDimensionGuard(int width, String expected) {
            assertEquals("https://my-publication.bbvms.com/mediaclip/1/spthumbnail/" + expected + "/default.webp",
                    offlineSdk().thumbnail().getMediaClipPosterPath(1, width));
        }

        @Test
        void posterUrlEscapesStringId() {
            assertEquals("https://my-publication.bbvms.com/mediaclip/a%20b%2Fc/spthumbnail/default/default.webp",
                    offlineSdk().thumbnail().getMediaClipPosterPath("a b/c"));
        }
    }
}
