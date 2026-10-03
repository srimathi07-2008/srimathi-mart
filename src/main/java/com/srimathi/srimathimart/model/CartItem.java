package com.srimathi.srimathimart.model;

import java.sql.Timestamp;

/** One buyer + product pairing in the persistent cart. */
public class CartItem {

    private long id;
    private long buyerId;
    private long productId;
    private int quantity;
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(final long value) {
        this.buyerId = value;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(final long value) {
        this.productId = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int value) {
        this.quantity = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
