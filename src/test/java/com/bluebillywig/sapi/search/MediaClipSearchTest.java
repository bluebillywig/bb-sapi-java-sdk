package com.bluebillywig.sapi.search;

import static com.bluebillywig.sapi.helpers.MockServer.MockResponse.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.helpers.SdkTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediaClipSearchTest extends SdkTestBase {
    @Test
    void sendsTheFiltersetAsJsonForSapiToCompile() {
        Fixture f = create(ok());

        f.sdk().mediaClip().search(FilterSet.create().where("status", FilterOperator.IS, "published"), 25, 50, "title asc", "holiday");

        String url = f.server().call(0).url();
        assertEquals("/sapi/mediaclip", pathOf(url));
        assertEquals("holiday", query(url, "q"));
        assertEquals("25", query(url, "limit"));
        assertEquals("50", query(url, "offset"));
        assertEquals("title asc", query(url, "sort"));
        assertEquals("[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]", query(url, "filterset"));
    }

    @Test
    void sendsNoFilterParametersWhenNothingIsFiltered() {
        Fixture f = create(ok());
        f.sdk().mediaClip().search(FilterSet.create());
        String url = f.server().call(0).url();
        assertNull(query(url, "filterset"));
        assertNull(query(url, "fq[0]"));
        assertEquals("*", query(url, "q"));
        assertEquals("15", query(url, "limit"));
    }

    @Test
    void encodesRawFilterQueriesAsIndexedParameters() {
        Fixture f = create(ok());

        f.sdk().mediaClip().search(FilterSet.create().where("status", FilterOperator.IS, "published"),
                15, 0, "createddate desc", "*", List.of("statusSort:\"published\"", "mediatype:video"));

        // SAPI accepts fq[0]= (percent-encoded fq%5B0%5D on the wire); it ignores a nested
        // fq[][0]= and a plain fq=, in both cases silently, so this encoding is pinned.
        String url = f.server().call(0).url();
        assertTrue(url.contains("fq%5B0%5D="));
        assertEquals("statusSort:\"published\"", query(url, "fq[0]"));
        assertEquals("mediatype:video", query(url, "fq[1]"));
        assertNull(query(url, "fq"));
        assertNotNull(query(url, "filterset"));
    }

    // Beyond the .NET suite: the raw bytes of the query as they hit the socket.

    @Test
    void queryIsEncodedOnTheWireWithPercentTwentyNeverPlus() {
        Fixture f = create(ok());

        f.sdk().mediaClip().search(FilterSet.create().where("status", FilterOperator.IS, "published"),
                15, 0, "createddate desc", "*", List.of("mediatype:video"));

        String url = f.server().call(0).url();
        assertTrue(url.contains("sort=createddate%20desc"), url);
        assertFalse(url.contains("+"), url);
        assertTrue(url.contains("q=%2A"), url);
        assertTrue(url.contains("fq%5B0%5D=mediatype%3Avideo"), url);
        assertTrue(url.contains("filterset=%5B%7B%22filters%22%3A%5B%7B%22field%22%3A%22status%22"), url);
    }

    @Test
    void listQueryIsEncodedOnTheWireWithPercentTwentyNeverPlus() {
        Fixture f = create(ok());
        f.sdk().mediaClip().list(15, 1, "createddate asc");
        assertEquals(f.server().url("/sapi/mediaclip?limit=15&offset=1&sort=createddate%20asc"), f.server().call(0).url());
    }
}
