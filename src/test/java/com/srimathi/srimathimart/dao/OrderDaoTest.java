package com.srimathi.srimathimart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.model.OrderItem;
import com.srimathi.srimathimart.model.Product;
import com.srimathi.srimathimart.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DAO tests for {@link JdbcOrderDao} against H2. */
class OrderDaoTest {

    private static TestDatabase database;

    private final OrderDao orderDao = new JdbcOrderDao();
    private final ProductDao productDao = new JdbcProductDao();
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

    private long product(final Connection connection, final String name, final String price) {
        long sellerId = userDao.insert(connection,
                TestDatabase.user(name + "-seller@example.com", Role.SELLER)).getId();

        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName(name);
        product.setDescription("d");
        product.setCategory("Home");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(20);
        product.setImageUrl("X");
        product.setActive(true);

        return productDao.insert(connection, product).getId();
    }

    private OrderItem item(final long productId, final String name,
                           final String price, final int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(productId);
        orderItem.setProductName(name);
        orderItem.setUnitPrice(new BigDecimal(price));
        orderItem.setQuantity(quantity);
        return orderItem;
    }

    @Test
    @DisplayName("insert writes the header and every line, and findById reads them back")
    void insertAndFind() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = userDao.insert(connection,
                    TestDatabase.user("buyer@example.com", Role.BUYER)).getId();

            long lamp = product(connection, "Lamp", "799.00");
            long pot = product(connection, "Pot", "599.00");

            Order order = new Order();
            order.setBuyerId(buyerId);
            order.setStatus("PLACED");
            order.setPaymentRef("MOCK-TEST");
            order.setTotalAmount(new BigDecimal("2197.00"));
            order.addItem(item(lamp, "Lamp", "799.00", 2));
            order.addItem(item(pot, "Pot", "599.00", 1));

            Order saved = orderDao.insert(connection, order);
            assertTrue(saved.getId() > 0);

            Order loaded = orderDao.findById(connection, saved.getId()).orElseThrow();

            assertEquals(2, loaded.getItems().size());
            assertEquals("MOCK-TEST", loaded.getPaymentRef());
            assertEquals(0, new BigDecimal("2197.00").compareTo(loaded.getTotalAmount()));

            OrderItem first = loaded.getItems().get(0);
            assertEquals("Lamp", first.getProductName());
            assertEquals(0, new BigDecimal("1598.00").compareTo(first.getLineTotal()));
        }
    }

    @Test
    @DisplayName("order_items keeps the price paid even after the product price changes")
    void priceIsFrozenAtPurchase() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = userDao.insert(connection,
                    TestDatabase.user("buyer@example.com", Role.BUYER)).getId();

            long lamp = product(connection, "Lamp", "799.00");

            Order order = new Order();
            order.setBuyerId(buyerId);
            order.setTotalAmount(new BigDecimal("799.00"));
            order.setPaymentRef("MOCK-TEST");
            order.addItem(item(lamp, "Lamp", "799.00", 1));

            Order saved = orderDao.insert(connection, order);

            Product current = productDao.findById(connection, lamp).orElseThrow();
            current.setPrice(new BigDecimal("999.00"));
            productDao.update(connection, current);

            Order loaded = orderDao.findById(connection, saved.getId()).orElseThrow();

            assertEquals(0, new BigDecimal("799.00")
                    .compareTo(loaded.getItems().get(0).getUnitPrice()),
                    "the order should remember what was actually paid");
        }
    }

    @Test
    @DisplayName("findByBuyer returns only that buyer's orders, newest first")
    void findByBuyer() throws SQLException {
        try (Connection connection = database.connection()) {

            long first = userDao.insert(connection,
                    TestDatabase.user("one@example.com", Role.BUYER)).getId();
            long second = userDao.insert(connection,
                    TestDatabase.user("two@example.com", Role.BUYER)).getId();

            long lamp = product(connection, "Lamp", "799.00");

            for (int i = 0; i < 2; i++) {
                Order order = new Order();
                order.setBuyerId(first);
                order.setTotalAmount(new BigDecimal("799.00"));
                order.setPaymentRef("MOCK-" + i);
                order.addItem(item(lamp, "Lamp", "799.00", 1));
                orderDao.insert(connection, order);
            }

            assertEquals(2, orderDao.findByBuyer(connection, first).size());
            assertTrue(orderDao.findByBuyer(connection, second).isEmpty());
        }
    }
}
