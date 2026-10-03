package com.srimathi.srimathimart.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** A catalogue item owned by exactly one seller. */
public class Product {

    private long id;
    private long sellerId;
    private String name;
    private String description = "";
    private BigDecimal price = BigDecimal.ZERO;
    private int stockQuantity;
    private String category;
    private String imageUrl = "";
    private boolean active = true;
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public long getSellerId() {
        return sellerId;
    }

    public void setSellerId(final long value) {
        this.sellerId = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        this.name = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String value) {
        this.description = value;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal value) {
        this.price = value;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(final int value) {
        this.stockQuantity = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String value) {
        this.category = value;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(final String value) {
        this.imageUrl = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean value) {
        this.active = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
