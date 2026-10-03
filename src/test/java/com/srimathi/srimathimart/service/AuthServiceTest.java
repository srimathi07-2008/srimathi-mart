package com.srimathi.srimathimart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.TestDatabase;
import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.dto.LoginRequest;
import com.srimathi.srimathimart.dto.RegisterRequest;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Service tests for {@link AuthService}, including the security rules. */
class AuthServiceTest {

    private static TestDatabase database;

    private final AuthService authService = new AuthService();
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

    private RegisterRequest signup(final String email, final Role role) {
        return new RegisterRequest("Test Person", email,
                "Password@123", "Password@123", role);
    }

    @Test
    @DisplayName("a buyer can register and then sign in")
    void registerThenLogin() {
        SessionUser registered = authService.register(signup("buyer@example.com", Role.BUYER));

        assertTrue(registered.getId() > 0);
        assertEquals(Role.BUYER, registered.getRole());

        SessionUser signedIn = authService.login(
                new LoginRequest("buyer@example.com", "Password@123", Role.BUYER));

        assertEquals(registered.getId(), signedIn.getId());
    }

    @Test
    @DisplayName("the stored password is a bcrypt hash, never the plaintext")
    void passwordIsHashed() throws SQLException {
        authService.register(signup("buyer@example.com", Role.BUYER));

        try (Connection connection = database.connection()) {
            User stored = userDao.findByEmail(connection, "buyer@example.com").orElseThrow();

            assertNotEquals("Password@123", stored.getPasswordHash());
            assertTrue(stored.getPasswordHash().startsWith("$2"),
                    "should be a bcrypt hash");
        }
    }

    @Test
    @DisplayName("two accounts with the same password get different hashes")
    void hashesAreSalted() throws SQLException {
        authService.register(signup("one@example.com", Role.BUYER));
        authService.register(signup("two@example.com", Role.BUYER));

        try (Connection connection = database.connection()) {
            String first = userDao.findByEmail(connection, "one@example.com")
                    .orElseThrow().getPasswordHash();
            String second = userDao.findByEmail(connection, "two@example.com")
                    .orElseThrow().getPasswordHash();

            assertNotEquals(first, second, "each hash must carry its own salt");
        }
    }

    @Test
    @DisplayName("registering as ADMIN is refused - there is no admin signup")
    void adminCannotSelfRegister() {
        ValidationException error = assertThrows(ValidationException.class, () ->
                authService.register(signup("admin@example.com", Role.ADMIN)));

        assertTrue(error.getMessage().toLowerCase().contains("administrator"));
    }

    @Test
    @DisplayName("a duplicate email is rejected")
    void duplicateEmailRejected() {
        authService.register(signup("buyer@example.com", Role.BUYER));

        assertThrows(ValidationException.class, () ->
                authService.register(signup("buyer@example.com", Role.BUYER)));
    }

    @Test
    @DisplayName("email is stored lower case, so sign-in is case-insensitive")
    void emailIsNormalised() {
        authService.register(new RegisterRequest("Test Person", "  Buyer@Example.COM  ",
                "Password@123", "Password@123", Role.BUYER));

        SessionUser signedIn = authService.login(
                new LoginRequest("buyer@example.com", "Password@123", Role.BUYER));

        assertEquals("buyer@example.com", signedIn.getEmail());
    }

    @Test
    @DisplayName("mismatched confirmation is rejected")
    void mismatchedConfirmation() {
        assertThrows(ValidationException.class, () ->
                authService.register(new RegisterRequest("Test Person", "buyer@example.com",
                        "Password@123", "Password@456", Role.BUYER)));
    }

    @Test
    @DisplayName("a short password is rejected")
    void weakPasswordRejected() {
        assertThrows(ValidationException.class, () ->
                authService.register(new RegisterRequest("Test Person", "buyer@example.com",
                        "short", "short", Role.BUYER)));
    }

    @Test
    @DisplayName("a malformed email is rejected")
    void malformedEmailRejected() {
        assertThrows(ValidationException.class, () ->
                authService.register(signup("not-an-email", Role.BUYER)));
    }

    @Test
    @DisplayName("a wrong password and an unknown account give the same message")
    void loginFailureDoesNotLeakAccountExistence() {
        authService.register(signup("buyer@example.com", Role.BUYER));

        AuthException wrongPassword = assertThrows(AuthException.class, () ->
                authService.login(new LoginRequest(
                        "buyer@example.com", "WrongPassword@1", Role.BUYER)));

        AuthException unknownAccount = assertThrows(AuthException.class, () ->
                authService.login(new LoginRequest(
                        "nobody@example.com", "WrongPassword@1", Role.BUYER)));

        assertEquals(wrongPassword.getMessage(), unknownAccount.getMessage(),
                "the two cases must be indistinguishable to a caller");
    }

    @Test
    @DisplayName("a buyer cannot sign in through the seller page")
    void roleIsEnforcedAtLogin() {
        authService.register(signup("buyer@example.com", Role.BUYER));

        assertThrows(AuthException.class, () ->
                authService.login(new LoginRequest(
                        "buyer@example.com", "Password@123", Role.SELLER)));
    }

    @Test
    @DisplayName("a deactivated account cannot sign in")
    void inactiveAccountRefused() throws SQLException {
        SessionUser registered = authService.register(signup("buyer@example.com", Role.BUYER));

        try (Connection connection = database.connection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET active = 0 WHERE id = ?")) {
            statement.setLong(1, registered.getId());
            statement.executeUpdate();
        }

        assertThrows(AuthException.class, () ->
                authService.login(new LoginRequest(
                        "buyer@example.com", "Password@123", Role.BUYER)));
    }

    @Test
    @DisplayName("a null expected role lets any role sign in")
    void nullRoleAllowsAny() {
        authService.register(signup("seller@example.com", Role.SELLER));

        SessionUser signedIn = authService.login(
                new LoginRequest("seller@example.com", "Password@123", null));

        assertEquals(Role.SELLER, signedIn.getRole());
    }
}
