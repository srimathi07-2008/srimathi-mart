package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.exception.DataAccessException;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.model.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link OrderDao}. */
public class JdbcOrderDao implements OrderDao {

    private static final String INSERT_ORDER_SQL =
            "INSERT INTO orders (buyer_id, total_amount, status, payment_ref) "
            + "VALUES (?, ?, ?, ?)";

    private static final String INSERT_ITEM_SQL =
            "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String FIND_ORDER_SQL =
            "SELECT id, buyer_id, total_amount, status, payment_ref, created_at "
            + "FROM orders WHERE id = ?";

    private static final String FIND_BY_BUYER_SQL =
            "SELECT id, buyer_id, total_amount, status, payment_ref, created_at "
            + "FROM orders WHERE buyer_id = ? ORDER BY id DESC";

    private static final String FIND_ITEMS_SQL =
            "SELECT id, order_id, product_id, product_name, unit_price, quantity, created_at "
            + "FROM order_items WHERE order_id = ? ORDER BY id ASC";

    @Override
    public Order insert(final Connection connection, final Order order) {
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    INSERT_ORDER_SQL, Statement.RETURN_GENERATED_KEYS)) {

                statement.setLong(1, order.getBuyerId());
                statement.setBigDecimal(2, order.getTotalAmount());
                statement.setString(3, order.getStatus());
                statement.setString(4, order.getPaymentRef());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        order.setId(keys.getLong(1));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(INSERT_ITEM_SQL)) {
                for (OrderItem item : order.getItems()) {
                    item.setOrderId(order.getId());
                    statement.setLong(1, order.getId());
                    statement.setLong(2, item.getProductId());
                    statement.setString(3, item.getProductName());
                    statement.setBigDecimal(4, item.getUnitPrice());
                    statement.setInt(5, item.getQuantity());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            return order;

        } catch (SQLException ex) {
            throw new DataAccessException("Could not save order.", ex);
        }
    }

    @Override
    public Optional<Order> findById(final Connection connection, final long orderId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_ORDER_SQL)) {
            statement.setLong(1, orderId);

            Order order;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                order = mapOrder(rs);
            }

            loadItems(connection, order);
            return Optional.of(order);

        } catch (SQLException ex) {
            throw new DataAccessException("Could not load order.", ex);
        }
    }

    @Override
    public List<Order> findByBuyer(final Connection connection, final long buyerId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_BUYER_SQL)) {
            statement.setLong(1, buyerId);

            try (ResultSet rs = statement.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                return orders;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not list orders.", ex);
        }
    }
@Override
public long countAll(final Connection connection) {
    String sql = "SELECT COUNT(*) FROM orders";

    try (PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {

        return rs.next() ? rs.getLong(1) : 0L;

    } catch (SQLException ex) {
        throw new DataAccessException("Could not count orders.", ex);
    }
}

@Override
public java.math.BigDecimal totalRevenue(final Connection connection) {
    String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders";

    try (PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {

        return rs.next()
                ? rs.getBigDecimal(1)
                : java.math.BigDecimal.ZERO;

    } catch (SQLException ex) {
        throw new DataAccessException("Could not calculate revenue.", ex);
    }
}
@Override
public long countBySeller(final Connection connection, final long sellerId) {
    String sql =
            "SELECT COUNT(DISTINCT o.id) "
            + "FROM orders o "
            + "JOIN order_items oi ON o.id = oi.order_id "
            + "JOIN products p ON oi.product_id = p.id "
            + "WHERE p.seller_id = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setLong(1, sellerId);

        try (ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }

    } catch (SQLException ex) {
        throw new DataAccessException(
                "Could not count seller orders.", ex);
    }
}

@Override
public java.math.BigDecimal revenueBySeller(
        final Connection connection,
        final long sellerId) {

    String sql =
            "SELECT COALESCE(SUM(oi.unit_price * oi.quantity), 0) "
            + "FROM order_items oi "
            + "JOIN orders o ON o.id = oi.order_id "
            + "JOIN products p ON oi.product_id = p.id "
            + "WHERE p.seller_id = ?";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setLong(1, sellerId);

        try (ResultSet rs = statement.executeQuery()) {
            return rs.next()
                    ? rs.getBigDecimal(1)
                    : java.math.BigDecimal.ZERO;
        }

    } catch (SQLException ex) {
        throw new DataAccessException(
                "Could not calculate seller revenue.", ex);
    }
}
    private void loadItems(final Connection connection, final Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_ITEMS_SQL)) {
            statement.setLong(1, order.getId());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getLong("id"));
                    item.setOrderId(rs.getLong("order_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setCreatedAt(rs.getTimestamp("created_at"));
                    order.addItem(item);
                }
            }
        }
    }

    private Order mapOrder(final ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setBuyerId(rs.getLong("buyer_id"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setPaymentRef(rs.getString("payment_ref"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }
}
