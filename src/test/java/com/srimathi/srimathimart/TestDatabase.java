package com.srimathi.srimathimart;

import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import com.srimathi.srimathimart.util.Db;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spins up a fresh in-memory H2 database behind a real HikariCP pool and
 * publishes it through {@link Db}, so the DAOs and services under test run
 * against exactly the same plumbing they use in production - only the JDBC
 * URL differs.
 *
 * <p>Each test class gets its own uniquely named database, so classes cannot
 * leak rows into one another even if the build runs them in parallel.</p>
 */
public final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final HikariDataSource dataSource;

    private TestDatabase(final HikariDataSource dataSourceValue) {
        this.dataSource = dataSourceValue;
    }

    /**
     * Creates a new in-memory database, applies schema-h2.sql, and installs
     * the pool into {@link Db}.
     *
     * @return the started database, to be closed when the test class ends
     */
    public static TestDatabase start() {
        // Keep bcrypt cheap: the default cost of 12 would make the auth tests
        // take seconds each for no extra coverage.
        System.setProperty("security.bcrypt.cost", "4");

        String name = "srimathi_test_" + COUNTER.incrementAndGet();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + name
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(4);
        config.setPoolName(name);

        HikariDataSource dataSource = new HikariDataSource(config);
        TestDatabase database = new TestDatabase(dataSource);

        Db.setDataSource(dataSource);
        database.applySchema();

        return database;
    }

    private void applySchema() {
        String sql = readResource("schema-h2.sql");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Could not apply the test schema.", ex);
        }
    }

    private static String readResource(final String name) {
        try (InputStream stream =
                     TestDatabase.class.getClassLoader().getResourceAsStream(name)) {

            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + name, ex);
        }
    }

    /** Deletes every row, leaving the schema in place. Call between tests. */
    public void truncate() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            for (String table : new String[] {
                    "reviews", "order_items", "orders",
                    "cart_items", "products", "users"}) {
                statement.execute("TRUNCATE TABLE " + table);
                statement.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1");
            }

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");

        } catch (SQLException ex) {
            throw new IllegalStateException("Could not truncate test tables.", ex);
        }
    }

    /**
     * Borrows a connection for a test to use directly.
     *
     * @return a pooled connection
     * @throws SQLException when the pool refuses
     */
    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Builds an unsaved user, ready to hand to a DAO.
     *
     * @param email the email
     * @param role  the role
     * @return the populated user
     */
    public static User user(final String email, final Role role) {
        User user = new User();
        user.setFullName("Test " + role.name());
        user.setEmail(email);
        user.setPasswordHash("$2a$04$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLM");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    /** Shuts the pool down. */
    public void stop() {
        Db.setDataSource(null);
        dataSource.close();
    }
}
