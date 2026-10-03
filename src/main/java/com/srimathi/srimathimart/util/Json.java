package com.srimathi.srimathimart.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A very small JSON writer.
 *
 * <p>The project deliberately avoids pulling in a JSON library: the payloads
 * here are a handful of flat objects, and hand-rolling the writer keeps the
 * dependency list to exactly what was specified. Every string value is escaped,
 * so a product name or description containing quotes or angle brackets cannot
 * break the output or inject markup into the page.</p>
 *
 * <p>Usage mirrors the shape of the JSON being produced:</p>
 * <pre>
 * new Json().beginObject()
 *           .put("ok", true)
 *           .name("items").beginArray()
 *               .beginObject().put("id", 1L).endObject()
 *           .endArray()
 *           .endObject()
 *           .toString();
 * </pre>
 */
public final class Json {

    private final StringBuilder out = new StringBuilder(256);

    /** One entry per open object or array; true once it holds a member. */
    private final Deque<Boolean> hasMembers = new ArrayDeque<>();

    /** True between name() and the value that follows it. */
    private boolean expectingValue;

    /**
     * Starts a JSON object.
     *
     * @return this writer
     */
    public Json beginObject() {
        prepareValue();
        out.append('{');
        hasMembers.push(Boolean.FALSE);
        return this;
    }

    /**
     * Ends the current JSON object.
     *
     * @return this writer
     */
    public Json endObject() {
        hasMembers.pop();
        out.append('}');
        return this;
    }

    /**
     * Starts a JSON array.
     *
     * @return this writer
     */
    public Json beginArray() {
        prepareValue();
        out.append('[');
        hasMembers.push(Boolean.FALSE);
        return this;
    }

    /**
     * Ends the current JSON array.
     *
     * @return this writer
     */
    public Json endArray() {
        hasMembers.pop();
        out.append(']');
        return this;
    }

    /**
     * Writes an object key. The next value written becomes its value.
     *
     * @param key the key
     * @return this writer
     */
    public Json name(final String key) {
        prepareValue();
        writeString(key);
        out.append(':');
        expectingValue = true;
        return this;
    }

    /**
     * Writes a string property.
     *
     * @param key   the key
     * @param value the value, null becomes an empty string
     * @return this writer
     */
    public Json put(final String key, final String value) {
        return name(key).value(value);
    }

    /**
     * Writes a numeric property.
     *
     * @param key   the key
     * @param value the value
     * @return this writer
     */
    public Json put(final String key, final long value) {
        return name(key).value(value);
    }

    /**
     * Writes a decimal property, always with two decimal places.
     *
     * @param key   the key
     * @param value the value, null becomes 0.00
     * @return this writer
     */
    public Json put(final String key, final BigDecimal value) {
        return name(key).value(value);
    }

    /**
     * Writes a boolean property.
     *
     * @param key   the key
     * @param value the value
     * @return this writer
     */
    public Json put(final String key, final boolean value) {
        return name(key).value(value);
    }

    /**
     * Writes a string value, as an array element or after {@link #name}.
     *
     * @param value the value, null becomes an empty string
     * @return this writer
     */
    public Json value(final String value) {
        prepareValue();
        writeString(value == null ? "" : value);
        return this;
    }

    /**
     * Writes a numeric value.
     *
     * @param value the value
     * @return this writer
     */
    public Json value(final long value) {
        prepareValue();
        out.append(value);
        return this;
    }

    /**
     * Writes a decimal value with two decimal places.
     *
     * @param value the value, null becomes 0.00
     * @return this writer
     */
    public Json value(final BigDecimal value) {
        prepareValue();
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        out.append(safe.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return this;
    }

    /**
     * Writes a boolean value.
     *
     * @param value the value
     * @return this writer
     */
    public Json value(final boolean value) {
        prepareValue();
        out.append(value);
        return this;
    }

    /**
     * Emits a separating comma when one is needed, then records that the
     * enclosing object or array now holds a member.
     */
    private void prepareValue() {
        if (expectingValue) {
            // This value belongs to the key just written - no comma, and the
            // enclosing object was already marked when the key was written.
            expectingValue = false;
            return;
        }
        if (hasMembers.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(hasMembers.peek())) {
            out.append(',');
        } else {
            hasMembers.pop();
            hasMembers.push(Boolean.TRUE);
        }
    }

    private void writeString(final String raw) {
        out.append('"');
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '<':
                    out.append("\\u003c");
                    break;
                case '>':
                    out.append("\\u003e");
                    break;
                case '&':
                    out.append("\\u0026");
                    break;
                default:
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                    break;
            }
        }
        out.append('"');
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
