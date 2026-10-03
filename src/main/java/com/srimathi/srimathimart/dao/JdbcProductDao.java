package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.dto.ProductSearch;
import com.srimathi.srimathimart.exception.DataAccessException;
import com.srimathi.srimathimart.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link ProductDao}.
 *
 * <p>The search method builds its WHERE clause from a fixed set of literal
 * fragments and always binds the user's keyword and category as parameters.
 * User input is never concatenated into SQL text.</p>
 */
public class JdbcProductDao implements ProductDao {

    private static final String COLUMNS =
            "id, seller_id, name, description, price, stock_quantity, "
            + "category, image_url, active, created_at";

    private static final String INSERT_SQL =
            "INSERT INTO products (seller_id, name, description, price, "
            + "stock_quantity, category, image_url, active) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE products SET name = ?, description = ?, price = ?, "
            + "stock_quantity = ?, category = ?, image_url = ?, active = ? "
            + "WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM products WHERE id = ?";

    private static final String FIND_BY_ID_SQL =
            "SELECT " + COLUMNS + " FROM products WHERE id = ?";

    private static final String FIND_BY_SELLER_SQL =
            "SELECT " + COLUMNS + " FROM products WHERE seller_id = ? ORDER BY id DESC";

    private static final String SEARCH_BASE_SQL =
            "SELECT " + COLUMNS + " FROM products WHERE active = TRUE";

    private static final String SEARCH_KEYWORD_SQL =
            " AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ? OR LOWER(category) LIKE ?)";

    private static final String SEARCH_CATEGORY_SQL = " AND LOWER(category) = ?";

    private static final String SEARCH_ORDER_SQL = " ORDER BY id DESC";

    private static final String CATEGORIES_SQL =
            "SELECT DISTINCT category FROM products WHERE active = TRUE ORDER BY category";

    private static final String DECREMENT_STOCK_SQL =
            "UPDATE products SET stock_quantity = stock_quantity - ? "
            + "WHERE id = ? AND stock_quantity >= ?";

    @Override
    public Product insert(final Connection connection, final Product product) {
        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, product.getSellerId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPrice());
            statement.setInt(5, product.getStockQuantity());
            statement.setString(6, product.getCategory());
            statement.setString(7, product.getImageUrl());
            statement.setBoolean(8, product.isActive());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setId(keys.getLong(1));
                }
            }
            return product;

        } catch (SQLException ex) {
            throw new DataAccessException("Could not insert product.", ex);
        }
    }

    @Override
    public boolean update(final Connection connection, final Product product) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStockQuantity());
            statement.setString(5, product.getCategory());
            statement.setString(6, product.getImageUrl());
            statement.setBoolean(7, product.isActive());
            statement.setLong(8, product.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update product.", ex);
        }
    }

    @Override
    public boolean delete(final Connection connection, final long productId) {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, productId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not delete product.", ex);
        }
    }

    @Override
    public Optional<Product> findById(final Connection connection, final long productId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load product.", ex);
        }
    }

    @Override
    public List<Product> findBySeller(final Connection connection, final long sellerId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_SELLER_SQL)) {
            statement.setLong(1, sellerId);
            return mapAll(statement);
        } catch (SQLException ex) {
            throw new DataAccessException("Could not list seller products.", ex);
        }
    }

    @Override
    public List<Product> search(final Connection connection, final ProductSearch search) {
        String keyword = normalise(search == null ? null : search.getKeyword());
        String category = normalise(search == null ? null : search.getCategory());
        boolean useCategory = category != null && !"all".equals(category);

        StringBuilder sql = new StringBuilder(SEARCH_BASE_SQL);
        if (keyword != null) {
            sql.append(SEARCH_KEYWORD_SQL);
        }
        if (useCategory) {
            sql.append(SEARCH_CATEGORY_SQL);
        }
        sql.append(SEARCH_ORDER_SQL);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (keyword != null) {
                String pattern = "%" + keyword + "%";
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
            }
            if (useCategory) {
                statement.setString(index, category);
            }
            return mapAll(statement);
        } catch (SQLException ex) {
            throw new DataAccessException("Could not search products.", ex);
        }
    }

    @Override
    public List<String> findCategories(final Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(CATEGORIES_SQL);
             ResultSet rs = statement.executeQuery()) {

            List<String> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
            return categories;

        } catch (SQLException ex) {
            throw new DataAccessException("Could not list categories.", ex);
        }
    }
@Override
public long countAll(final Connection connection) {
    String sql = "SELECT COUNT(*) FROM products";

    try (PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {

        return rs.next() ? rs.getLong(1) : 0L;

    } catch (SQLException ex) {
        throw new DataAccessException("Could not count products.", ex);
    }
}
    @Override
    public boolean decrementStock(final Connection connection,
                                  final long productId,
                                  final int quantity) {
        try (PreparedStatement statement = connection.prepareStatement(DECREMENT_STOCK_SQL)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update stock.", ex);
        }
    }

    private static String normalise(final String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<Product> mapAll(final PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(map(rs));
            }
            return products;
        }
    }

    private Product map(final ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setSellerId(rs.getLong("seller_id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setCategory(rs.getString("category"));
        product.setImageUrl(rs.getString("image_url"));
        product.setActive(rs.getBoolean("active"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        return product;
    }
}
