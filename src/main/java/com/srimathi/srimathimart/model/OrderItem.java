package com.srimathi.srimathimart.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** A frozen line of a placed order. Name and price are copied at purchase time. */
public class OrderItem {

    private long id;
    private long orderId;
    private long productId;
    private String productName;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private int quantity;
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(final long value) {
        this.orderId = value;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(final long value) {
        this.productId = value;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(final String value) {
        this.productName = value;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(final BigDecimal value) {
        this.unitPrice = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int value) {
        this.quantity = value;
    }

    /**
     * Line total for this row.
     *
     * @return unit price multiplied by quantity
     */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
