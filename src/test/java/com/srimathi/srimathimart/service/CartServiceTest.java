package com.srimathi.srimathimart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.dto.CartView;
import com.srimathi.srimathimart.dto.ProductForm;
import com.srimathi.srimathimart.exception.NotFoundException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Service tests for {@link CartService}, including the stock guard. */
class CartServiceTest {

    private static TestDatabase database;

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

    @Test
    @DisplayName("add puts a line in the cart and reports the running total")
    void addComputesTotal() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        CartView cart = cartService.add(buyerId, lamp, 2);

        assertEquals(1, cart.getDistinctCount());
        assertEquals(2, cart.getTotalQuantity());
        assertEquals(0, new BigDecimal("1598.00").compareTo(cart.getSubtotal()));
    }

    @Test
    @DisplayName("the running total spans several lines")
    void totalAcrossLines() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);

        cartService.add(buyerId, product("Lamp", "799.00", "10"), 2);
        CartView cart = cartService.add(buyerId, product("Pot", "599.00", "10"), 1);

        assertEquals(2, cart.getDistinctCount());
        assertEquals(3, cart.getTotalQuantity());
        assertEquals(0, new BigDecimal("2197.00").compareTo(cart.getSubtotal()));
    }

    @Test
    @DisplayName("adding the same product again increases its quantity")
    void addTwiceIncrements() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(buyerId, lamp, 1);
        CartView cart = cartService.add(buyerId, lamp, 2);

        assertEquals(1, cart.getDistinctCount());
        assertEquals(3, cart.getTotalQuantity());
    }

    @Test
    @DisplayName("you cannot add more than the stock on hand")
    void stockIsEnforced() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "3");

        assertThrows(ValidationException.class, () -> cartService.add(buyerId, lamp, 4));

        // The guard must also cover a second add that pushes the total over.
        cartService.add(buyerId, lamp, 2);
        assertThrows(ValidationException.class, () -> cartService.add(buyerId, lamp, 2));

        assertEquals(2, cartService.view(buyerId).getTotalQuantity());
    }

    @Test
    @DisplayName("a quantity below one is rejected")
    void quantityMustBePositive() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        assertThrows(ValidationException.class, () -> cartService.add(buyerId, lamp, 0));
        assertThrows(ValidationException.class, () -> cartService.add(buyerId, lamp, -1));
    }

    @Test
    @DisplayName("adding a product that does not exist reports not found")
    void unknownProduct() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);

        assertThrows(NotFoundException.class, () -> cartService.add(buyerId, 9999L, 1));
    }

    @Test
    @DisplayName("updateQuantity sets an exact value and zero removes the line")
    void updateQuantity() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(buyerId, lamp, 1);

        assertEquals(5, cartService.updateQuantity(buyerId, lamp, 5).getTotalQuantity());

        CartView emptied = cartService.updateQuantity(buyerId, lamp, 0);
        assertTrue(emptied.getLines().isEmpty());
    }

    @Test
    @DisplayName("updateQuantity also respects stock")
    void updateRespectsStock() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "3");

        cartService.add(buyerId, lamp, 1);

        assertThrows(ValidationException.class,
                () -> cartService.updateQuantity(buyerId, lamp, 99));
    }

    @Test
    @DisplayName("remove takes a line out and clear empties the cart")
    void removeAndClear() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");
        long pot = product("Pot", "599.00", "10");

        cartService.add(buyerId, lamp, 1);
        cartService.add(buyerId, pot, 1);

        assertEquals(1, cartService.remove(buyerId, lamp).getDistinctCount());

        cartService.clear(buyerId);
        assertTrue(cartService.view(buyerId).getLines().isEmpty());
    }

    @Test
    @DisplayName("an empty cart has a zero subtotal, not a null one")
    void emptyCart() throws SQLException {
        long buyerId = user("buyer@example.com", Role.BUYER);

        CartView cart = cartService.view(buyerId);

        assertEquals(0, cart.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getSubtotal()));
    }

    @Test
    @DisplayName("carts are per buyer")
    void cartsAreIsolated() throws SQLException {
        long first = user("one@example.com", Role.BUYER);
        long second = user("two@example.com", Role.BUYER);
        long lamp = product("Lamp", "799.00", "10");

        cartService.add(first, lamp, 3);

        assertEquals(3, cartService.view(first).getTotalQuantity());
        assertEquals(0, cartService.view(second).getTotalQuantity());
    }
}
