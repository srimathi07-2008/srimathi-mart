package com.srimathi.srimathimart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dto.ProductSearch;
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

/** DAO tests for {@link JdbcProductDao} against H2. */
class ProductDaoTest {

    private static TestDatabase database;

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

    private long seller(final Connection connection, final String email) {
        return userDao.insert(connection, TestDatabase.user(email, Role.SELLER)).getId();
    }

    private Product product(final long sellerId,
                            final String name,
                            final String category,
                            final String price,
                            final int stock) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setImageUrl("X");
        product.setActive(true);
        return product;
    }

    @Test
    @DisplayName("insert, findById and update round-trip every column")
    void crud() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");

            Product saved = productDao.insert(connection,
                    product(sellerId, "Wireless Headphones", "Electronics", "1499.00", 24));

            assertTrue(saved.getId() > 0);

            Product loaded = productDao.findById(connection, saved.getId()).orElseThrow();
            assertEquals("Wireless Headphones", loaded.getName());
            assertEquals(0, new BigDecimal("1499.00").compareTo(loaded.getPrice()));
            assertEquals(24, loaded.getStockQuantity());
            assertEquals("Electronics", loaded.getCategory());

            loaded.setName("Wireless Headphones Pro");
            loaded.setPrice(new BigDecimal("1799.00"));
            assertTrue(productDao.update(connection, loaded));

            Product updated = productDao.findById(connection, saved.getId()).orElseThrow();
            assertEquals("Wireless Headphones Pro", updated.getName());
            assertEquals(0, new BigDecimal("1799.00").compareTo(updated.getPrice()));
        }
    }

    @Test
    @DisplayName("delete removes the row")
    void delete() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            Product saved = productDao.insert(connection,
                    product(sellerId, "Desk Lamp", "Home", "799.00", 25));

            assertTrue(productDao.delete(connection, saved.getId()));
            assertTrue(productDao.findById(connection, saved.getId()).isEmpty());
        }
    }

    @Test
    @DisplayName("findBySeller returns only that seller's products")
    void findBySeller() throws SQLException {
        try (Connection connection = database.connection()) {

            long first = seller(connection, "one@example.com");
            long second = seller(connection, "two@example.com");

            productDao.insert(connection, product(first, "Item A", "Home", "100.00", 5));
            productDao.insert(connection, product(first, "Item B", "Home", "200.00", 5));
            productDao.insert(connection, product(second, "Item C", "Home", "300.00", 5));

            assertEquals(2, productDao.findBySeller(connection, first).size());
            assertEquals(1, productDao.findBySeller(connection, second).size());
        }
    }

    @Test
    @DisplayName("search with no filters returns the whole active catalogue")
    void searchAll() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            productDao.insert(connection, product(sellerId, "Sneakers", "Fashion", "1899.00", 10));
            productDao.insert(connection, product(sellerId, "Kurta", "Fashion", "1099.00", 10));

            assertEquals(2, productDao.search(connection, new ProductSearch(null, null)).size());
            assertEquals(2, productDao.search(connection, new ProductSearch("", "")).size());
        }
    }

    @Test
    @DisplayName("search matches a keyword case-insensitively across name and description")
    void searchKeyword() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            productDao.insert(connection,
                    product(sellerId, "Wireless Headphones", "Electronics", "1499.00", 10));
            productDao.insert(connection,
                    product(sellerId, "Plant Pot", "Home", "599.00", 10));

            List<Product> byName =
                    productDao.search(connection, new ProductSearch("HEADPHONES", null));
            assertEquals(1, byName.size());
            assertEquals("Wireless Headphones", byName.get(0).getName());

            // "Description for Plant Pot" - matched through the description column.
            List<Product> byDescription =
                    productDao.search(connection, new ProductSearch("description for plant", null));
            assertEquals(1, byDescription.size());
        }
    }

    @Test
    @DisplayName("search filters by category, and 'all' means no filter")
    void searchCategory() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            productDao.insert(connection, product(sellerId, "Sneakers", "Fashion", "1899.00", 10));
            productDao.insert(connection, product(sellerId, "Lamp", "Home", "799.00", 10));

            assertEquals(1, productDao.search(connection,
                    new ProductSearch(null, "Fashion")).size());

            // Case should not matter.
            assertEquals(1, productDao.search(connection,
                    new ProductSearch(null, "fashion")).size());

            assertEquals(2, productDao.search(connection,
                    new ProductSearch(null, "All")).size());
        }
    }

    @Test
    @DisplayName("a quote in the keyword is bound, not interpolated into the SQL")
    void searchIsInjectionSafe() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            productDao.insert(connection, product(sellerId, "Lamp", "Home", "799.00", 10));

            // If this were concatenated the statement would fail or drop the
            // table. Because it is bound, it simply matches nothing.
            List<Product> results = productDao.search(connection,
                    new ProductSearch("'; DROP TABLE products; --", null));

            assertTrue(results.isEmpty());

            // Proves the table is still there.
            assertEquals(1, productDao.search(connection, new ProductSearch(null, null)).size());
        }
    }

    @Test
    @DisplayName("inactive products are hidden from the buyer catalogue")
    void searchSkipsInactive() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            Product hidden = product(sellerId, "Retired Item", "Home", "100.00", 5);
            hidden.setActive(false);
            productDao.insert(connection, hidden);

            assertTrue(productDao.search(connection, new ProductSearch(null, null)).isEmpty());
        }
    }

    @Test
    @DisplayName("findCategories lists each category once, sorted")
    void findCategories() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            productDao.insert(connection, product(sellerId, "A", "Home", "10.00", 1));
            productDao.insert(connection, product(sellerId, "B", "Home", "10.00", 1));
            productDao.insert(connection, product(sellerId, "C", "Fashion", "10.00", 1));

            assertEquals(List.of("Fashion", "Home"), productDao.findCategories(connection));
        }
    }

    @Test
    @DisplayName("decrementStock deducts when stock allows and refuses when it does not")
    void decrementStock() throws SQLException {
        try (Connection connection = database.connection()) {

            long sellerId = seller(connection, "seller@example.com");
            Product saved = productDao.insert(connection,
                    product(sellerId, "Handbag", "Accessories", "1249.00", 5));

            assertTrue(productDao.decrementStock(connection, saved.getId(), 3));
            assertEquals(2, productDao.findById(connection, saved.getId())
                    .orElseThrow().getStockQuantity());

            // Asking for more than remains must fail and must not change stock.
            assertFalse(productDao.decrementStock(connection, saved.getId(), 3));
            assertEquals(2, productDao.findById(connection, saved.getId())
                    .orElseThrow().getStockQuantity());
        }
    }
}
