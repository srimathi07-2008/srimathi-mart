package com.srimathi.srimathimart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.dto.ProductForm;
import com.srimathi.srimathimart.dto.ProductSearch;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.exception.NotFoundException;
import com.srimathi.srimathimart.exception.ValidationException;
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

/** Service tests for {@link ProductService}, including ownership rules. */
class ProductServiceTest {

    private static TestDatabase database;

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

    private long seller(final String email) throws SQLException {
        try (Connection connection = database.connection()) {
            return userDao.insert(connection,
                    TestDatabase.user(email, Role.SELLER)).getId();
        }
    }

    private ProductForm form(final String name, final String price,
                             final String stock, final String category) {
        return new ProductForm(null, name, "A description", price, stock, category, "🎧");
    }

    @Test
    @DisplayName("create stores every field")
    void create() throws SQLException {
        long sellerId = seller("seller@example.com");

        Product saved = productService.create(sellerId,
                form("Wireless Headphones", "1499.00", "24", "Electronics"));

        assertTrue(saved.getId() > 0);
        assertEquals("Wireless Headphones", saved.getName());
        assertEquals(24, saved.getStockQuantity());
        assertEquals("Electronics", saved.getCategory());
        assertEquals(0, new BigDecimal("1499.00").compareTo(saved.getPrice()));
    }

    @Test
    @DisplayName("a price typed with a rupee sign and separators is still accepted")
    void priceIsForgiving() throws SQLException {
        long sellerId = seller("seller@example.com");

        Product saved = productService.create(sellerId,
                form("Sneakers", "₹1,899", "10", "Fashion"));

        assertEquals(0, new BigDecimal("1899.00").compareTo(saved.getPrice()));
    }

    @Test
    @DisplayName("a missing name or a bad price is rejected")
    void validation() throws SQLException {
        long sellerId = seller("seller@example.com");

        assertThrows(ValidationException.class, () ->
                productService.create(sellerId, form("", "100", "1", "Home")));

        assertThrows(ValidationException.class, () ->
                productService.create(sellerId, form("Lamp", "abc", "1", "Home")));

        assertThrows(ValidationException.class, () ->
                productService.create(sellerId, form("Lamp", "-5", "1", "Home")));

        assertThrows(ValidationException.class, () ->
                productService.create(sellerId, form("Lamp", "100", "-1", "Home")));

        assertThrows(ValidationException.class, () ->
                productService.create(sellerId, form("Lamp", "100", "1", "")));
    }

    @Test
    @DisplayName("update changes the listing")
    void update() throws SQLException {
        long sellerId = seller("seller@example.com");
        Product saved = productService.create(sellerId, form("Lamp", "799", "25", "Home"));

        Product updated = productService.update(sellerId, saved.getId(),
                form("Desk Lamp", "899", "30", "Home"));

        assertEquals("Desk Lamp", updated.getName());
        assertEquals(30, updated.getStockQuantity());
        assertEquals(0, new BigDecimal("899.00").compareTo(updated.getPrice()));
    }

    @Test
    @DisplayName("a seller cannot edit another seller's product")
    void updateIsOwnershipChecked() throws SQLException {
        long owner = seller("owner@example.com");
        long intruder = seller("intruder@example.com");

        Product saved = productService.create(owner, form("Lamp", "799", "25", "Home"));

        assertThrows(AuthException.class, () ->
                productService.update(intruder, saved.getId(),
                        form("Stolen Lamp", "1", "1", "Home")));

        // The listing must be untouched.
        assertEquals("Lamp", productService.get(saved.getId()).getName());
    }

    @Test
    @DisplayName("a seller cannot delete another seller's product")
    void deleteIsOwnershipChecked() throws SQLException {
        long owner = seller("owner@example.com");
        long intruder = seller("intruder@example.com");

        Product saved = productService.create(owner, form("Lamp", "799", "25", "Home"));

        assertThrows(AuthException.class, () ->
                productService.delete(intruder, saved.getId()));

        assertEquals("Lamp", productService.get(saved.getId()).getName());
    }

    @Test
    @DisplayName("delete removes the seller's own product")
    void delete() throws SQLException {
        long sellerId = seller("seller@example.com");
        Product saved = productService.create(sellerId, form("Lamp", "799", "25", "Home"));

        productService.delete(sellerId, saved.getId());

        assertThrows(NotFoundException.class, () -> productService.get(saved.getId()));
    }

    @Test
    @DisplayName("acting on a product that does not exist reports not found")
    void missingProduct() throws SQLException {
        long sellerId = seller("seller@example.com");

        assertThrows(NotFoundException.class, () ->
                productService.update(sellerId, 9999L, form("Lamp", "1", "1", "Home")));

        assertThrows(NotFoundException.class, () ->
                productService.delete(sellerId, 9999L));
    }

    @Test
    @DisplayName("browse supports keyword and category filtering")
    void browse() throws SQLException {
        long sellerId = seller("seller@example.com");

        productService.create(sellerId, form("Wireless Headphones", "1499", "10", "Electronics"));
        productService.create(sellerId, form("Everyday Sneakers", "1899", "10", "Fashion"));

        assertEquals(2, productService.browse(new ProductSearch(null, null)).size());
        assertEquals(1, productService.browse(new ProductSearch("sneakers", null)).size());
        assertEquals(1, productService.browse(new ProductSearch(null, "Electronics")).size());
        assertEquals(0, productService.browse(new ProductSearch("nothing", null)).size());
    }

    @Test
    @DisplayName("listForSeller shows only that seller's own listings")
    void listForSeller() throws SQLException {
        long first = seller("one@example.com");
        long second = seller("two@example.com");

        productService.create(first, form("Lamp", "799", "10", "Home"));
        productService.create(second, form("Pot", "599", "10", "Home"));

        assertEquals(1, productService.listForSeller(first).size());
        assertEquals("Lamp", productService.listForSeller(first).get(0).getName());
    }

    @Test
    @DisplayName("categories are de-duplicated")
    void categories() throws SQLException {
        long sellerId = seller("seller@example.com");

        productService.create(sellerId, form("Lamp", "799", "10", "Home"));
        productService.create(sellerId, form("Pot", "599", "10", "Home"));
        productService.create(sellerId, form("Kurta", "1099", "10", "Fashion"));

        assertEquals(2, productService.categories().size());
    }
}
