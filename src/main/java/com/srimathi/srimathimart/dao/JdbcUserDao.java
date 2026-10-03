package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.exception.DataAccessException;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link UserDao}.
 *
 * <p>Every statement below is a {@link PreparedStatement} with bound
 * parameters. No SQL string in this class is ever built by concatenating
 * caller-supplied values.</p>
 */
public class JdbcUserDao implements UserDao {

    private static final String INSERT_SQL =
            "INSERT INTO users (full_name, email, password_hash, role, active) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BASE =
            "SELECT id, full_name, email, password_hash, role, active, created_at FROM users ";

    private static final String FIND_BY_EMAIL_SQL = SELECT_BASE + "WHERE email = ?";

    private static final String FIND_BY_ID_SQL = SELECT_BASE + "WHERE id = ?";

    private static final String EXISTS_BY_EMAIL_SQL =
            "SELECT 1 FROM users WHERE email = ?";

    private static final String UPDATE_PASSWORD_SQL =
            "UPDATE users SET password_hash = ? WHERE id = ?";

    private static final String COUNT_BY_ROLE_SQL =
            "SELECT COUNT(*) FROM users WHERE role = ?";

    @Override
    public User insert(final Connection connection, final User user) {
        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isActive());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;

        } catch (SQLException ex) {
            throw new DataAccessException("Could not insert user.", ex);
        }
    }

    @Override
    public Optional<User> findByEmail(final Connection connection, final String email) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL_SQL)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load user by email.", ex);
        }
    }

    @Override
    public Optional<User> findById(final Connection connection, final long id) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load user by id.", ex);
        }
    }

    @Override
    public boolean existsByEmail(final Connection connection, final String email) {
        try (PreparedStatement statement = connection.prepareStatement(EXISTS_BY_EMAIL_SQL)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not check email availability.", ex);
        }
    }

    @Override
    public boolean updatePasswordHash(final Connection connection,
                                      final long userId,
                                      final String passwordHash) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD_SQL)) {
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update password.", ex);
        }
    }
@Override
public long countAll(final Connection connection) {
    String sql = "SELECT COUNT(*) FROM users";

    try (PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {

        return rs.next() ? rs.getLong(1) : 0L;

    } catch (SQLException ex) {
        throw new DataAccessException("Could not count users.", ex);
    }
}
    @Override
    public long countByRole(final Connection connection, final String role) {
        try (PreparedStatement statement = connection.prepareStatement(COUNT_BY_ROLE_SQL)) {
            statement.setString(1, role);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not count users by role.", ex);
        }
    }
@Override
public List<User> findRecentUsers(final Connection connection) {
    String sql =
            "SELECT id, full_name, email, password_hash, role, active, created_at "
            + "FROM users ORDER BY id DESC";

    try (PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {

        List<User> users = new ArrayList<>();

        while (rs.next()) {
            users.add(map(rs));
        }

        return users;

    } catch (SQLException ex) {
        throw new DataAccessException("Could not load recent users.", ex);
    }
}
    private User map(final ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.from(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
