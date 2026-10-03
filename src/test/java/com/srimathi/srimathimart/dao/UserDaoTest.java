package com.srimathi.srimathimart.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DAO tests for {@link JdbcUserDao} against H2. */
class UserDaoTest {

    private static TestDatabase database;

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

    @Test
    @DisplayName("insert assigns a generated id and the row can be read back")
    void insertAndFind() throws SQLException {
        try (Connection connection = database.connection()) {

            User saved = userDao.insert(connection,
                    TestDatabase.user("buyer@example.com", Role.BUYER));

            assertTrue(saved.getId() > 0, "id should be generated");

            Optional<User> found = userDao.findByEmail(connection, "buyer@example.com");

            assertTrue(found.isPresent());
            assertEquals(Role.BUYER, found.get().getRole());
            assertEquals("buyer@example.com", found.get().getEmail());
            assertTrue(found.get().isActive());
        }
    }

    @Test
    @DisplayName("findById returns the same row insert created")
    void findById() throws SQLException {
        try (Connection connection = database.connection()) {

            User saved = userDao.insert(connection,
                    TestDatabase.user("seller@example.com", Role.SELLER));

            Optional<User> found = userDao.findById(connection, saved.getId());

            assertTrue(found.isPresent());
            assertEquals("seller@example.com", found.get().getEmail());
        }
    }

    @Test
    @DisplayName("an unknown email yields an empty Optional, not null")
    void findByEmailMissing() throws SQLException {
        try (Connection connection = database.connection()) {
            assertTrue(userDao.findByEmail(connection, "nobody@example.com").isEmpty());
        }
    }

    @Test
    @DisplayName("existsByEmail reflects what is actually stored")
    void existsByEmail() throws SQLException {
        try (Connection connection = database.connection()) {

            assertFalse(userDao.existsByEmail(connection, "buyer@example.com"));

            userDao.insert(connection, TestDatabase.user("buyer@example.com", Role.BUYER));

            assertTrue(userDao.existsByEmail(connection, "buyer@example.com"));
        }
    }

    @Test
    @DisplayName("the UNIQUE(email) constraint rejects a duplicate registration")
    void duplicateEmailRejected() throws SQLException {
        try (Connection connection = database.connection()) {

            userDao.insert(connection, TestDatabase.user("dupe@example.com", Role.BUYER));

            assertThrows(RuntimeException.class, () ->
                    userDao.insert(connection,
                            TestDatabase.user("dupe@example.com", Role.SELLER)));
        }
    }

    @Test
    @DisplayName("updatePasswordHash replaces the stored hash")
    void updatePasswordHash() throws SQLException {
        try (Connection connection = database.connection()) {

            User saved = userDao.insert(connection,
                    TestDatabase.user("admin@example.com", Role.ADMIN));

            boolean updated = userDao.updatePasswordHash(
                    connection, saved.getId(), "$2a$04$replacementhashvaluegoeshere0123456789ABCDEFG");

            assertTrue(updated);

            Optional<User> found = userDao.findById(connection, saved.getId());
            assertTrue(found.isPresent());
            assertTrue(found.get().getPasswordHash().startsWith("$2a$04$replacement"));
        }
    }

    @Test
    @DisplayName("countByRole counts only the requested role")
    void countByRole() throws SQLException {
        try (Connection connection = database.connection()) {

            userDao.insert(connection, TestDatabase.user("b1@example.com", Role.BUYER));
            userDao.insert(connection, TestDatabase.user("b2@example.com", Role.BUYER));
            userDao.insert(connection, TestDatabase.user("s1@example.com", Role.SELLER));

            assertEquals(2, userDao.countByRole(connection, "BUYER"));
            assertEquals(1, userDao.countByRole(connection, "SELLER"));
            assertEquals(0, userDao.countByRole(connection, "ADMIN"));
        }
    }
}
