package com.bluebillywig.sapi.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bluebillywig.sapi.util.SapiJson;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The filterset wire format. These pin the SHAPE, because the shape is the contract; the
 * semantics are SAPI's to own. The Node SDK verified equivalence against a live publication:
 * sending a filterset and a hand-compiled fq return identical counts.
 */
class FilterSetTest {
    private static Object firstValue(FilterSet filterSet) {
        return filterSet.toArray().get(0).getFilters().get(0).getValue();
    }

    @Test
    void oneConditionIsAGroupOfOne() {
        List<FilterGroup> groups = FilterSet.create().where("status", FilterOperator.IS, "published").toArray();

        assertEquals(1, groups.size());
        assertEquals(1, groups.get(0).getFilters().size());
        Filter filter = groups.get(0).getFilters().get(0);
        assertEquals("status", filter.getField());
        assertEquals(FilterOperator.IS, filter.getOperator());
        assertEquals("published", filter.getValue());
        assertNull(filter.getType());
    }

    @Test
    void eachWhereIsItsOwnGroup() {
        FilterSet filterSet = FilterSet.create().where("status", FilterOperator.IS, "published").where("mediatype", FilterOperator.IS, "video");
        assertEquals(2, filterSet.toArray().size());
    }

    @Test
    void filtersPassedTogetherShareAGroup() {
        FilterSet filterSet = FilterSet.create().andGroup(
                new Filter("status", FilterOperator.IS, "published"),
                new Filter("status", FilterOperator.IS, "draft"));

        List<FilterGroup> groups = filterSet.toArray();
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getFilters().size());
    }

    @Test
    void carriesEntityTypeButOmitsItWhenAbsent() {
        assertEquals("mediaclip", FilterSet.create().where("status", FilterOperator.IS, "published", "mediaclip").toArray().get(0).getFilters().get(0).getType());
        assertNull(FilterSet.create().where("status", FilterOperator.IS, "published").toArray().get(0).getFilters().get(0).getType());
        assertFalse(FilterSet.create().where("status", FilterOperator.IS, "published").toJson().contains("type"));
    }

    @Test
    void carriesSeveralValuesAsAList() {
        Object value = firstValue(FilterSet.create().where("status", FilterOperator.IS_ANY_OF, new String[] {"published", "draft"}));
        assertEquals(List.of("published", "draft"), value);
    }

    @Test
    void dropsAFilterWithNothingToMatchOn() {
        assertTrue(FilterSet.create().where("status", FilterOperator.IS, "   ").toArray().isEmpty());
        assertTrue(FilterSet.create().where("status", FilterOperator.IS).isEmpty());
        assertTrue(FilterSet.create().where("status", FilterOperator.IS, new String[0]).isEmpty());
    }

    @Test
    void sendsThePlaceholderForPresenceOperators() {
        // The backend's compiler skips ANY filter whose value is empty, presence tests included,
        // so a bare isEmpty silently never fires. OVP6 sends '*'; the SDK must do the same.
        FilterSet filterSet = FilterSet.create().where("author", FilterOperator.IS_EMPTY);

        assertFalse(filterSet.isEmpty());
        assertEquals("*", firstValue(filterSet));
    }

    @Test
    void placeholderOverridesSuppliedValue() {
        assertEquals("*", firstValue(FilterSet.create().where("author", FilterOperator.IS_NOT_EMPTY, "anything")));
    }

    @Test
    void normalisesNumbersAndBooleansToStrings() {
        // A JSON number works, but a JSON boolean gets mangled into "1" by the backend and matches
        // nothing; a boolean must go out as the string 'true'/'false'.
        assertEquals("100", firstValue(FilterSet.create().where("views", FilterOperator.IS_GREATER_THAN, 100)));
        assertEquals("true", firstValue(FilterSet.create().where("hasInteractivity", FilterOperator.IS, true)));
        assertEquals("false", firstValue(FilterSet.create().where("isImported", FilterOperator.IS, false)));
        assertEquals("2.5", firstValue(FilterSet.create().where("ratio", FilterOperator.IS, 2.5)));
        Object list = firstValue(FilterSet.create().where("views", FilterOperator.IS_ANY_OF, new Object[] {1, 2.5, true}));
        assertEquals(List.of("1", "2.5", "true"), list);
    }

    @Test
    void dropsNonScalarArrayMembers() {
        Object value = firstValue(FilterSet.from("[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":[\"published\",{\"nested\":true}]}]}]"));
        assertEquals(List.of("published"), value);
    }

    @Test
    void skipsAGroupWithoutAFiltersArray() {
        FilterSet filterSet = FilterSet.from("[{\"notFilters\":true},{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]");
        assertEquals("[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]", filterSet.toJson());
    }

    @Test
    void toleratesJunkShapesWithoutCrashing() {
        assertTrue(FilterSet.from("{\"type\":\"SearchRequest\",\"filterSet\":\"junk\"}").isEmpty());
        assertTrue(FilterSet.from("[{\"filters\":[null]}]").isEmpty());
        assertTrue(FilterSet.from("null").isEmpty());
        assertTrue(FilterSet.from("[{\"filters\":[{\"operator\":\"is\",\"value\":\"x\"}]}]").isEmpty());
    }

    @Test
    void unknownOperatorIsADescriptiveError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FilterSet.from("[{\"filters\":[{\"field\":\"status\",\"operator\":\"Is\",\"value\":\"x\"}]}]"));
        assertTrue(ex.getMessage().contains("Unknown filter operator 'Is'"));
        assertTrue(ex.getMessage().contains("isNot"));
    }

    @Test
    void dropsAGroupLeftWithNoFilters() {
        List<FilterGroup> groups = FilterSet.create()
                .andGroup(new Filter("status", FilterOperator.IS, ""))
                .where("mediatype", FilterOperator.IS, "video")
                .toArray();

        assertEquals(1, groups.size());
        assertEquals("mediatype", groups.get(0).getFilters().get(0).getField());
    }

    @Test
    void serialisesToTheJsonSapiExpects() {
        assertEquals("[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]",
                FilterSet.create().where("status", FilterOperator.IS, "published").toString());
    }

    @Test
    void roundTripsFromEnvelopeAndBareList() {
        final String groups = "[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]";
        assertEquals(groups, FilterSet.from("{\"type\":\"SearchRequest\",\"filterSet\":" + groups + "}").toJson());
        assertEquals(groups, FilterSet.from(groups).toJson());
        assertEquals(groups, FilterSet.from(SapiJson.readTree(groups)).toJson());
    }

    @Test
    void ingestedNumbersKeepTheirLiteral() {
        assertEquals("[{\"filters\":[{\"field\":\"views\",\"operator\":\"isGreaterThan\",\"value\":\"2.50\"}]}]",
                FilterSet.from("[{\"filters\":[{\"field\":\"views\",\"operator\":\"isGreaterThan\",\"value\":2.50}]}]").toJson());
    }

    @Test
    void serialisesThroughTheJsonMapperAsTheWireFormat() {
        FilterSet filterSet = FilterSet.create().where("status", FilterOperator.IS, "published");
        assertEquals("{\"filterset\":[{\"filters\":[{\"field\":\"status\",\"operator\":\"is\",\"value\":\"published\"}]}]}",
                SapiJson.write(Map.of("filterset", filterSet)));
        assertEquals(filterSet.toJson(), SapiJson.read(filterSet.toJson(), FilterSet.class).toJson());
    }

    @Test
    void envelopeWithNoGroupsIsEmpty() {
        assertTrue(FilterSet.from("{\"type\":\"SearchRequest\"}").isEmpty());
    }

    @Test
    void isImmutable() {
        FilterSet a = FilterSet.create();
        FilterSet b = a.where("status", FilterOperator.IS, "published");
        assertTrue(a.isEmpty());
        assertFalse(b.isEmpty());
    }

    @Test
    void wireNamesRoundTrip() {
        for (FilterOperator op : FilterOperator.values()) {
            assertEquals(op, FilterOperator.parse(op.toWire()));
        }
        assertEquals(17, FilterOperator.wireNames().size());
    }

    // Beyond the .NET suite.

    @Test
    void deserialisedEnvelopeKeepsNumberLiteralsToo() {
        String json = "{\"type\":\"SearchRequest\",\"filterSet\":[{\"filters\":[{\"field\":\"views\",\"operator\":\"isGreaterThan\",\"value\":1e3}]}]}";
        assertEquals("[{\"filters\":[{\"field\":\"views\",\"operator\":\"isGreaterThan\",\"value\":\"1e3\"}]}]",
                SapiJson.read(json, FilterSet.class).toJson());
    }

    @Test
    void ingestedBooleansAndMixedListsAreNormalised() {
        FilterSet filterSet = FilterSet.from("[{\"filters\":[{\"field\":\"a\",\"operator\":\"isAnyOf\",\"value\":[true,false,3,\"x\"],\"type\":\"mediaclip\"}]}]");
        assertEquals("[{\"filters\":[{\"field\":\"a\",\"operator\":\"isAnyOf\",\"value\":[\"true\",\"false\",\"3\",\"x\"],\"type\":\"mediaclip\"}]}]",
                filterSet.toJson());
    }

    @Test
    void invalidJsonIsAnUncheckedIoException() {
        assertThrows(UncheckedIOException.class, () -> FilterSet.from("[{"));
        assertThrows(UncheckedIOException.class, () -> FilterSet.from("[] []"));
        assertThrows(UncheckedIOException.class, () -> FilterSet.from(""));
    }

    @Test
    void falseAndZeroSurviveAsStrings() {
        // The scalar '0' is dropped by the backend, but the SDK must not drop it itself.
        assertEquals("0", firstValue(FilterSet.create().where("views", FilterOperator.IS, 0)));
        assertEquals("false", firstValue(FilterSet.create().where("isImported", FilterOperator.IS, false)));
    }

    @Test
    void nonBreakingSpaceCountsAsBlank() {
        assertTrue(FilterSet.create().where("status", FilterOperator.IS, "  \t").isEmpty());
    }
}
