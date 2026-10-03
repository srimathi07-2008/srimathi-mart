package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.dto.CartLine;
import com.srimathi.srimathimart.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link CartDao}.
 *
 * <p>The add path is written as UPDATE-then-INSERT rather than a MySQL-only
 * {@code ON DUPLICATE KEY UPDATE}, so the same statements run unchanged
 * against H2 in the DAO tests.</p>
 */
public class JdbcCartDao implements CartDao {

    private static final String INCREMENT_SQL =
            "UPDATE cart_items SET quantity = quantity + ? "
            + "WHERE buyer_id = ? AND product_id = ?";

    private static final String INSERT_SQL =
            "INSERT INTO cart_items (buyer_id, product_id, quantity) VALUES (?, ?, ?)";

    private static final String UPDATE_QUANTITY_SQL =
            "UPDATE cart_items SET quantity = ? WHERE buyer_id = ? AND product_id = ?";

    private static final String REMOVE_SQL =
            "DELETE FROM cart_items WHERE buyer_id = ? AND product_id = ?";

    private static final String CLEAR_SQL = "DELETE FROM cart_items WHERE buyer_id = ?";

    private static final String FIND_LINES_SQL =
            "SELECT c.product_id, c.quantity, p.name, p.image_url, p.price, p.stock_quantity "
            + "FROM cart_items c "
            + "JOIN products p ON p.id = c.product_id "
            + "WHERE c.buyer_id = ? "
            + "ORDER BY c.id ASC";

    @Override
    public void addOrIncrement(final Connection connection,
                               final long buyerId,
                               final long productId,
                               final int quantity) {
        try {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(INCREMENT_SQL)) {
                statement.setInt(1, quantity);
                statement.setLong(2, buyerId);
                statement.setLong(3, productId);
                updated = statement.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                    statement.setLong(1, buyerId);
                    statement.setLong(2, productId);
                    statement.setInt(3, quantity);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not add item to cart.", ex);
        }
    }

    @Override
    public boolean updateQuantity(final Connection connection,
                                  final long buyerId,
                                  final long productId,
                                  final int quantity) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_QUANTITY_SQL)) {
            statement.setInt(1, quantity);
            statement.setLong(2, buyerId);
            statement.setLong(3, productId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update cart quantity.", ex);
        }
    }

    @Override
    public boolean remove(final Connection connection,
                          final long buyerId,
                          final long productId) {
        try (PreparedStatement statement = connection.prepareStatement(REMOVE_SQL)) {
            statement.setLong(1, buyerId);
            statement.setLong(2, productId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not remove cart item.", ex);
        }
    }

    @Override
    public int clear(final Connection connection, final long buyerId) {
        try (PreparedStatement statement = connection.prepareStatement(CLEAR_SQL)) {
            statement.setLong(1, buyerId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not clear cart.", ex);
        }
    }

    @Override
    public List<CartLine> findLines(final Connection connection, final long buyerId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LINES_SQL)) {
            statement.setLong(1, buyerId);

            try (ResultSet rs = statement.executeQuery()) {
                List<CartLine> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(new CartLine(
                            rs.getLong("product_id"),
                            rs.getString("name"),
                            rs.getString("image_url"),
                            rs.getBigDecimal("price"),
                            rs.getInt("quantity"),
                            rs.getInt("stock_quantity")));
                }
                return lines;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load cart.", ex);
        }
    }
}
