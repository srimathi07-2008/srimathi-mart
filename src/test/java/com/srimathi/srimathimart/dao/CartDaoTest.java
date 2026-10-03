package com.srimathi.srimathimart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dto.CartLine;
import com.srimathi.srimathimart.model.Product;
import com.srimathi.srimathimart.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DAO tests for {@link JdbcCartDao} against H2. */
class CartDaoTest {

    private static TestDatabase database;

    private final CartDao cartDao = new JdbcCartDao();
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

    private long buyer(final Connection connection, final String email) {
        return userDao.insert(connection, TestDatabase.user(email, Role.BUYER)).getId();
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
        product.setStockQuantity(50);
        product.setImageUrl("X");
        product.setActive(true);

        return productDao.insert(connection, product).getId();
    }

    @Test
    @DisplayName("adding the same product twice increments rather than duplicating")
    void addOrIncrement() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = buyer(connection, "buyer@example.com");
            long productId = product(connection, "Lamp", "799.00");

            cartDao.addOrIncrement(connection, buyerId, productId, 1);
            cartDao.addOrIncrement(connection, buyerId, productId, 2);

            List<CartLine> lines = cartDao.findLines(connection, buyerId);

            assertEquals(1, lines.size(), "UNIQUE(buyer_id, product_id) means one row");
            assertEquals(3, lines.get(0).getQuantity());
        }
    }

    @Test
    @DisplayName("findLines joins product name, price and stock onto each row")
    void findLinesJoinsProduct() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = buyer(connection, "buyer@example.com");
            long productId = product(connection, "Handbag", "1249.00");

            cartDao.addOrIncrement(connection, buyerId, productId, 2);

            CartLine line = cartDao.findLines(connection, buyerId).get(0);

            assertEquals("Handbag", line.getName());
            assertEquals(0, new BigDecimal("1249.00").compareTo(line.getUnitPrice()));
            assertEquals(50, line.getStockQuantity());
            assertEquals(0, new BigDecimal("2498.00").compareTo(line.getLineTotal()));
        }
    }

    @Test
    @DisplayName("updateQuantity sets an exact value")
    void updateQuantity() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = buyer(connection, "buyer@example.com");
            long productId = product(connection, "Lamp", "799.00");

            cartDao.addOrIncrement(connection, buyerId, productId, 1);
            assertTrue(cartDao.updateQuantity(connection, buyerId, productId, 7));

            assertEquals(7, cartDao.findLines(connection, buyerId).get(0).getQuantity());
        }
    }

    @Test
    @DisplayName("remove deletes one line, clear empties the cart")
    void removeAndClear() throws SQLException {
        try (Connection connection = database.connection()) {

            long buyerId = buyer(connection, "buyer@example.com");
            long first = product(connection, "Lamp", "799.00");
            long second = product(connection, "Pot", "599.00");

            cartDao.addOrIncrement(connection, buyerId, first, 1);
            cartDao.addOrIncrement(connection, buyerId, second, 1);

            assertTrue(cartDao.remove(connection, buyerId, first));
            assertEquals(1, cartDao.findLines(connection, buyerId).size());

            assertEquals(1, cartDao.clear(connection, buyerId));
            assertTrue(cartDao.findLines(connection, buyerId).isEmpty());
        }
    }

    @Test
    @DisplayName("one buyer's cart is invisible to another")
    void cartsAreIsolated() throws SQLException {
        try (Connection connection = database.connection()) {

            long first = buyer(connection, "one@example.com");
            long second = buyer(connection, "two@example.com");
            long productId = product(connection, "Lamp", "799.00");

            cartDao.addOrIncrement(connection, first, productId, 4);

            assertEquals(1, cartDao.findLines(connection, first).size());
            assertTrue(cartDao.findLines(connection, second).isEmpty());
        }
    }
}
