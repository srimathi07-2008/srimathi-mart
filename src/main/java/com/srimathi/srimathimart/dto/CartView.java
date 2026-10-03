package com.srimathi.srimathimart.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/** The whole cart plus its running totals. */
public class CartView {

    private final List<CartLine> lines;

    /**
     * Creates a cart view.
     *
     * @param lineValues the cart lines
     */
    public CartView(final List<CartLine> lineValues) {
        this.lines = lineValues;
    }

    public List<CartLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Number of distinct product rows.
     *
     * @return row count
     */
    public int getDistinctCount() {
        return lines.size();
    }

    /**
     * Sum of all quantities.
     *
     * @return total item count
     */
    public int getTotalQuantity() {
        int total = 0;
        for (CartLine line : lines) {
            total += line.getQuantity();
        }
        return total;
    }

    /**
     * Running subtotal across every line.
     *
     * @return subtotal scaled to two decimals
     */
    public BigDecimal getSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : lines) {
            total = total.add(line.getLineTotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
