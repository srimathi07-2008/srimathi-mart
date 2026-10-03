package com.srimathi.srimathimart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.dto.ProductForm;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Service tests for {@link OrderService}: checkout, stock and rollback. */
class OrderServiceTest {

    private static TestDatabase database;

    private final OrderService orderService = new OrderService();
    private final CartService cartService = new CartService();
    private final ProductService productService = new ProductService();
    private final UserDao userDao = new JdbcUserDao();

    @BeforeAll
    static void startDatabase() {
        database = TestDatabase.start();
    }

    @AfterEach
    void clean() {
        database.truncate();
    }

    @AfterAll
    static void stopDatabase() {
        database.stop();
    }

    private long user(final String email, final Role role) throws SQLException {
        try (Connection connection = database.connection()) {
            return userDao.insert(connection, TestDatabase.user(email, role)).getId();
        }
    }

    private long product(final String name, final String price, final String stock)
            throws SQLException {
        long sellerId = user(name + "-seller@example.com", Role.SELLER);
        return productService.create(sellerId,
                new ProductForm(null, name, "d", price, stock, "Home", "X")).getId();
    }

    /** Forces a stock value behind the service's back, to simulate a race. */
    private void setStock(final long productId, final int stock) throws SQLException {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE products SET stock_quantity = ? WHERE id = ?")) {
            statement.setInt(1, stock);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    @Test
    @DisplayName("checkout writes the order, its lines and a mock payment reference")
    void placeOrder() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");
        long pot = product("Pot", "599.00", "10");

        cartService.add(buyerId, lamp, 2);
        cartService.add(buyerId, pot, 1);

        Order order = orderService.placeOrderFromCart(buyerId,"COD");

        assertTrue(order.getId() > 0);
        assertEquals("PLACED", order.getStatus());
        assertEquals(2, order.getItems().size());
        assertEquals(0, new BigDecimal("2197.00").compareTo(order.getTotalAmount()));
        assertTrue(order.getPaymentRef().startsWith("MOCK-"),
                "payment is mocked, and the reference should say so");
    }

    @Test
    @DisplayName("checkout deducts stock and empties the cart")
    void checkoutDeductsStockAndClearsCart() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(buyerId, lamp, 3);
        orderService.placeOrderFromCart(buyerId,"COD");

        assertEquals(7, productService.get(lamp).getStockQuantity());
        assertTrue(cartService.view(buyerId).getLines().isEmpty());
    }

    @Test
    @DisplayName("checking out an empty cart is refused")
    void emptyCartRefused() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);

        assertThrows(ValidationException.class,
                () -> orderService.placeOrderFromCart(buyerId,"COD"));
    }

    @Test
    @DisplayName("if one line went out of stock the whole checkout rolls back")
    void outOfStockRollsBackEverything() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");
        long pot = product("Pot", "599.00", "10");

        cartService.add(buyerId, lamp, 2);
        cartService.add(buyerId, pot, 2);

        // Someone else bought the pots in the meantime.
        setStock(pot, 1);

        assertThrows(ValidationException.class,
                () -> orderService.placeOrderFromCart(buyerId,"COD"));

        // Nothing may have been committed: the lamp stock must be untouched,
        // the cart must still be intact, and no order may exist.
        assertEquals(10, productService.get(lamp).getStockQuantity());
        assertEquals(2, cartService.view(buyerId).getDistinctCount());
        assertTrue(orderService.listForBuyer(buyerId).isEmpty());
    }

    @Test
    @DisplayName("each order gets its own payment reference")
    void paymentReferencesDiffer() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(buyerId, lamp, 1);
        String first = orderService.placeOrderFromCart(buyerId,"COD").getPaymentRef();

        cartService.add(buyerId, lamp, 1);
        String second = orderService.placeOrderFromCart(buyerId,"COD").getPaymentRef();

        assertTrue(!first.equals(second));
    }

    @Test
    @DisplayName("listForBuyer returns only that buyer's orders")
    void listForBuyer() throws SQLException {
        long first = user("one@example.com", Role.BUYER);
        long second = user("two@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(first, lamp, 1);
        orderService.placeOrderFromCart(first,"COD");

        assertEquals(1, orderService.listForBuyer(first).size());
        assertTrue(orderService.listForBuyer(second).isEmpty());
    }
}
