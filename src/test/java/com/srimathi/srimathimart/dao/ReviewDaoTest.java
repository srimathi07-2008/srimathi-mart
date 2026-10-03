package com.srimathi.srimathimart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.model.Product;
import com.srimathi.srimathimart.model.Review;
import com.srimathi.srimathimart.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DAO tests for {@link JdbcReviewDao} against H2. */
class ReviewDaoTest {

    private static TestDatabase database;

    private final ReviewDao reviewDao = new JdbcReviewDao();
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

    private long product(final Connection connection) {
        long sellerId = userDao.insert(connection,
                TestDatabase.user("seller@example.com", Role.SELLER)).getId();

        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName("Lamp");
        product.setDescription("d");
        product.setCategory("Home");
        product.setPrice(new BigDecimal("799.00"));
        product.setStockQuantity(10);
        product.setImageUrl("X");
        product.setActive(true);

        return productDao.insert(connection, product).getId();
    }

    private Review review(final long productId, final long buyerId, final int rating) {
        Review review = new Review();
        review.setProductId(productId);
        review.setBuyerId(buyerId);
        review.setRating(rating);
        review.setComment("Comment " + rating);
        return review;
    }

    @Test
    @DisplayName("insert then findByProduct returns the review")
    void insertAndFind() throws SQLException {
        try (Connection connection = database.connection()) {

            long productId = product(connection);
            long buyerId = userDao.insert(connection,
                    TestDatabase.user("buyer@example.com", Role.BUYER)).getId();

            reviewDao.insert(connection, review(productId, buyerId, 5));

            assertEquals(1, reviewDao.findByProduct(connection, productId).size());
            assertEquals(5, reviewDao.findByProduct(connection, productId).get(0).getRating());
        }
    }

    @Test
    @DisplayName("averageRating averages every review and returns 0 when there are none")
    void averageRating() throws SQLException {
        try (Connection connection = database.connection()) {

            long productId = product(connection);

            assertEquals(0d, reviewDao.averageRating(connection, productId), 0.0001,
                    "no reviews should read as 0, not null");

            long first = userDao.insert(connection,
                    TestDatabase.user("one@example.com", Role.BUYER)).getId();
            long second = userDao.insert(connection,
                    TestDatabase.user("two@example.com", Role.BUYER)).getId();

            reviewDao.insert(connection, review(productId, first, 5));
            reviewDao.insert(connection, review(productId, second, 3));

            assertEquals(4d, reviewDao.averageRating(connection, productId), 0.0001);
        }
    }

    @Test
    @DisplayName("a buyer cannot review the same product twice")
    void oneReviewPerBuyer() throws SQLException {
        try (Connection connection = database.connection()) {

            long productId = product(connection);
            long buyerId = userDao.insert(connection,
                    TestDatabase.user("buyer@example.com", Role.BUYER)).getId();

            reviewDao.insert(connection, review(productId, buyerId, 5));

            assertThrows(RuntimeException.class, () ->
                    reviewDao.insert(connection, review(productId, buyerId, 1)));
        }
    }
}
