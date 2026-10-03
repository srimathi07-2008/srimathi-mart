package com.srimathi.srimathimart.dto;

/** Raw product fields as submitted by the seller dashboard. */
public class ProductForm {

    private final Long id;
    private final String name;
    private final String description;
    private final String price;
    private final String stockQuantity;
    private final String category;
    private final String imageUrl;

    /**
     * Creates a product form.
     *
     * @param idValue            existing product id, null when creating
     * @param nameValue          product name
     * @param descriptionValue   product description
     * @param priceValue         price as raw text
     * @param stockQuantityValue stock as raw text
     * @param categoryValue      category name
     * @param imageUrlValue      image URL or emoji glyph
     */
    public ProductForm(final Long idValue,
                       final String nameValue,
                       final String descriptionValue,
                       final String priceValue,
                       final String stockQuantityValue,
                       final String categoryValue,
                       final String imageUrlValue) {
        this.id = idValue;
        this.name = nameValue;
        this.description = descriptionValue;
        this.price = priceValue;
        this.stockQuantity = stockQuantityValue;
        this.category = categoryValue;
        this.imageUrl = imageUrlValue;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getStockQuantity() {
        return stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
