package com.srimathi.srimathimart.dto;

/** Buyer-facing catalogue filter. Both fields are optional. */
public class ProductSearch {

    private final String keyword;
    private final String category;

    /**
     * Creates a catalogue filter.
     *
     * @param keywordValue  free text matched against name and description
     * @param categoryValue exact category name, or null / "All"
     */
    public ProductSearch(final String keywordValue, final String categoryValue) {
        this.keyword = keywordValue;
        this.category = categoryValue;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCategory() {
        return category;
    }
}
