package com.srimathi.srimathimart.model;

import java.sql.Timestamp;

/** A buyer's rating and comment on a product. */
public class Review {

    private long id;
    private long productId;
    private long buyerId;
    private int rating;
    private String comment = "";
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(final long value) {
        this.productId = value;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(final long value) {
        this.buyerId = value;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(final int value) {
        this.rating = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String value) {
        this.comment = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
