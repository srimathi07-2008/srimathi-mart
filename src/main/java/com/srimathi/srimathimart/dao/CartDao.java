package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.dto.CartLine;
import java.sql.Connection;
import java.util.List;

/** Persistence for {@code cart_items}. */
public interface CartDao {

    /**
     * Adds a product to a cart, or increases the quantity when it is already
     * there. Relies on the UNIQUE(buyer_id, product_id) constraint.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @param productId  the product id
     * @param quantity   the amount to add
     */
    void addOrIncrement(Connection connection, long buyerId, long productId, int quantity);

    /**
     * Sets an exact quantity for one cart row.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @param productId  the product id
     * @param quantity   the new quantity, must be positive
     * @return true when a row was updated
     */
    boolean updateQuantity(Connection connection, long buyerId, long productId, int quantity);

    /**
     * Removes one product from a cart.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @param productId  the product id
     * @return true when a row was deleted
     */
    boolean remove(Connection connection, long buyerId, long productId);

    /**
     * Empties a cart, used after a successful checkout.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @return the number of rows removed
     */
    int clear(Connection connection, long buyerId);

    /**
     * Loads the cart joined with product details.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @return the cart lines, oldest first
     */
    List<CartLine> findLines(Connection connection, long buyerId);
}
