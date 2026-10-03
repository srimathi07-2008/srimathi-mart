package com.srimathi.srimathimart.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the hand-rolled {@link Json} writer. Every servlet response
 * goes through this class, so its comma placement and escaping are worth
 * pinning down.
 */
class JsonTest {

    @Test
    @DisplayName("an empty object and array serialise correctly")
    void empties() {
        assertEquals("{}", new Json().beginObject().endObject().toString());
        assertEquals("[]", new Json().beginArray().endArray().toString());
    }

    @Test
    @DisplayName("commas separate members but never lead or trail")
    void commaPlacement() {
        String json = new Json()
                .beginObject()
                .put("a", 1L)
                .put("b", 2L)
                .put("c", 3L)
                .endObject()
                .toString();

        assertEquals("{\"a\":1,\"b\":2,\"c\":3}", json);
    }

    @Test
    @DisplayName("nested objects and arrays keep their own comma state")
    void nesting() {
        String json = new Json()
                .beginObject()
                .put("ok", true)
                .name("items").beginArray()
                    .beginObject().put("id", 1L).endObject()
                    .beginObject().put("id", 2L).endObject()
                .endArray()
                .put("count", 2L)
                .endObject()
                .toString();

        assertEquals(
                "{\"ok\":true,\"items\":[{\"id\":1},{\"id\":2}],\"count\":2}",
                json);
    }

    @Test
    @DisplayName("string array elements are comma separated")
    void stringArray() {
        String json = new Json()
                .beginArray()
                .value("Fashion")
                .value("Home")
                .endArray()
                .toString();

        assertEquals("[\"Fashion\",\"Home\"]", json);
    }

    @Test
    @DisplayName("decimals always carry two places")
    void decimalScale() {
        String json = new Json()
                .beginObject()
                .put("price", new BigDecimal("1499"))
                .put("zero", (BigDecimal) null)
                .endObject()
                .toString();

        assertEquals("{\"price\":1499.00,\"zero\":0.00}", json);
    }

    @Test
    @DisplayName("quotes and backslashes are escaped so the output stays parseable")
    void escapesQuotes() {
        String json = new Json()
                .beginObject()
                .put("name", "12\" \\ lamp")
                .endObject()
                .toString();

        assertEquals("{\"name\":\"12\\\" \\\\ lamp\"}", json);
    }

    @Test
    @DisplayName("angle brackets are escaped, so a product name cannot inject markup")
    void escapesMarkup() {
        String json = new Json()
                .beginObject()
                .put("name", "<script>alert(1)</script>")
                .endObject()
                .toString();

        assertFalse(json.contains("<"), "raw < must never reach the page");
        assertFalse(json.contains(">"), "raw > must never reach the page");
        assertTrue(json.contains("\\u003c"));
    }

    @Test
    @DisplayName("newlines and control characters are escaped")
    void escapesControlCharacters() {
        String json = new Json()
                .beginObject()
                .put("text", "line1\nline2\tend")
                .endObject()
                .toString();

        assertEquals("{\"text\":\"line1\\nline2\\tend\"}", json);
    }

    @Test
    @DisplayName("a null string becomes an empty string, not the word null")
    void nullString() {
        String json = new Json()
                .beginObject()
                .put("name", (String) null)
                .endObject()
                .toString();

        assertEquals("{\"name\":\"\"}", json);
    }

    @Test
    @DisplayName("a rupee sign and emoji survive unescaped")
    void keepsUnicode() {
        String json = new Json()
                .beginObject()
                .put("image", "🎧")
                .endObject()
                .toString();

        assertTrue(json.contains("🎧"));
    }
}
