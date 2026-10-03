package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.exception.DataAccessException;
import com.srimathi.srimathimart.model.Review;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation of {@link ReviewDao}. */
public class JdbcReviewDao implements ReviewDao {

    private static final String INSERT_SQL =
            "INSERT INTO reviews (product_id, buyer_id, rating, comment) VALUES (?, ?, ?, ?)";

    private static final String FIND_BY_PRODUCT_SQL =
            "SELECT id, product_id, buyer_id, rating, comment, created_at "
            + "FROM reviews WHERE product_id = ? ORDER BY id DESC";

    private static final String AVERAGE_SQL =
            "SELECT AVG(rating) FROM reviews WHERE product_id = ?";

    @Override
    public Review insert(final Connection connection, final Review review) {
        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, review.getProductId());
            statement.setLong(2, review.getBuyerId());
            statement.setInt(3, review.getRating());
            statement.setString(4, review.getComment());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setId(keys.getLong(1));
                }
            }
            return review;

        } catch (SQLException ex) {
            throw new DataAccessException("Could not insert review.", ex);
        }
    }

    @Override
    public List<Review> findByProduct(final Connection connection, final long productId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_PRODUCT_SQL)) {
            statement.setLong(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    Review review = new Review();
                    review.setId(rs.getLong("id"));
                    review.setProductId(rs.getLong("product_id"));
                    review.setBuyerId(rs.getLong("buyer_id"));
                    review.setRating(rs.getInt("rating"));
                    review.setComment(rs.getString("comment"));
                    review.setCreatedAt(rs.getTimestamp("created_at"));
                    reviews.add(review);
                }
                return reviews;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not list reviews.", ex);
        }
    }

    @Override
    public double averageRating(final Connection connection, final long productId) {
        try (PreparedStatement statement = connection.prepareStatement(AVERAGE_SQL)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    double average = rs.getDouble(1);
                    return rs.wasNull() ? 0d : average;
                }
                return 0d;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not compute average rating.", ex);
        }
    }
}
