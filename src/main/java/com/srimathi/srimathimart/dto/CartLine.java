package com.srimathi.srimathimart.dto;

import java.math.BigDecimal;

/** A cart row joined with its product, ready for display. */
public class CartLine {

    private final long productId;
    private final String name;
    private final String imageUrl;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final int stockQuantity;

    /**
     * Creates a cart display line.
     *
     * @param productIdValue     product id
     * @param nameValue          product name
     * @param imageUrlValue      image URL or emoji glyph
     * @param unitPriceValue     current unit price
     * @param quantityValue      quantity in the cart
     * @param stockQuantityValue stock currently available
     */
    public CartLine(final long productIdValue,
                    final String nameValue,
                    final String imageUrlValue,
                    final BigDecimal unitPriceValue,
                    final int quantityValue,
                    final int stockQuantityValue) {
        this.productId = productIdValue;
        this.name = nameValue;
        this.imageUrl = imageUrlValue;
        this.unitPrice = unitPriceValue;
        this.quantity = quantityValue;
        this.stockQuantity = stockQuantityValue;
    }

    public long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Total for this line.
     *
     * @return unit price multiplied by quantity
     */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
