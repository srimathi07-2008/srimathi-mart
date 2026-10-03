package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.model.Order;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Persistence for {@code orders} and {@code order_items}. */
public interface OrderDao {

    /**
     * Inserts an order header plus every line, inside the caller's transaction.
     *
     * @param connection an open connection already in a transaction
     * @param order      the order with its items populated
     * @return the order with generated ids populated
     */
    Order insert(Connection connection, Order order);

    /**
     * Finds an order and its items.
     *
     * @param connection an open connection
     * @param orderId    the order id
     * @return the order, if present
     */
    Optional<Order> findById(Connection connection, long orderId);

    /**
     * Lists a buyer's orders, newest first, without their items.
     *
     * @param connection an open connection
     * @param buyerId    the buyer id
     * @return the buyer's orders
     */
    List<Order> findByBuyer(Connection connection, long buyerId);
    long countAll(Connection connection);

java.math.BigDecimal totalRevenue(Connection connection);
}
