package com.srimathi.srimathimart.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A placed order. Payment is mock only - no gateway is contacted. */
public class Order {

    private long id;
    private long buyerId;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String status = "PLACED";
    private String paymentRef = "";
    private String paymentMethod="COD";
    private Timestamp createdAt;
    private final List<OrderItem> items = new ArrayList<>();

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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(final BigDecimal value) {
        this.totalAmount = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String value) {
        this.status = value;
    }

    public String getPaymentRef() {
        return paymentRef;
    }
public String getPaymentMethod() {
    return paymentMethod;
}

public void setPaymentMethod(final String value) {
    this.paymentMethod = value;
}
    public void setPaymentRef(final String value) {
        this.paymentRef = value;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Adds a line to this order.
     *
     * @param item the line to add
     */
    public void addItem(final OrderItem item) {
        this.items.add(item);
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
