package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.dto.LoginRequest;
import com.srimathi.srimathimart.dto.RegisterRequest;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import com.srimathi.srimathimart.util.PasswordUtil;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registration and login rules.
 *
 * <p>Two deliberate choices here. First, ADMIN is rejected at registration no
 * matter what the request says, so there is no admin signup path even if
 * someone posts {@code role=ADMIN} directly. Second, a failed login always
 * reports the same generic message whether the email was unknown or the
 * password was wrong, so the response cannot be used to enumerate accounts.</p>
 */
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");

    private static final String GENERIC_LOGIN_FAILURE =
            "Incorrect email or password.";

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_EMAIL_LENGTH = 190;

    private final UserDao userDao;

    /** Creates the service with the default JDBC DAO. */
    public AuthService() {
        this(new JdbcUserDao());
    }

    /**
     * Creates the service with a supplied DAO, which is how the tests inject
     * an H2-backed implementation.
     *
     * @param userDaoValue the user DAO
     */
    public AuthService(final UserDao userDaoValue) {
        this.userDao = userDaoValue;
    }

    /**
     * Registers a buyer or seller.
     *
     * @param request the signup form
     * @return the new account as a session projection
     */
    public SessionUser register(final RegisterRequest request) {
        String fullName = requireText(request.getFullName(), "Full name");
        String email = normaliseEmail(request.getEmail());
        Role role = request.getRole();

        if (fullName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Full name is too long.");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Email address is too long.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (role == null) {
            throw new ValidationException("Choose an account type.");
        }
        if (!Role.isSelfRegisterable(role)) {
            // Covers role=ADMIN posted directly at the endpoint.
            throw new ValidationException("Administrator accounts cannot be self registered.");
        }
        if (request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("The two passwords do not match.");
        }

        PasswordUtil.validateStrength(request.getPassword());

        return TransactionTemplate.inTransaction(connection -> {
            if (userDao.existsByEmail(connection, email)) {
                throw new ValidationException("That email is already registered.");
            }

            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
            user.setRole(role);
            user.setActive(true);

            User saved = userDao.insert(connection, user);
            return toSessionUser(saved);
        });
    }

    /**
     * Verifies credentials.
     *
     * @param request the login form
     * @return the authenticated account as a session projection
     */
    public SessionUser login(final LoginRequest request) {
        String email = normaliseEmail(request.getEmail());
        String password = request.getPassword();

        if (email.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Enter your email and password.");
        }

        return TransactionTemplate.read(connection -> {
            Optional<User> found = userDao.findByEmail(connection, email);

            if (found.isEmpty()) {
                // Hash a throwaway value so a missing account takes roughly as
                // long as a wrong password, removing the timing signal.
                PasswordUtil.matches(password,
                        "$2a$10$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvwxyz01234");
                throw new AuthException(GENERIC_LOGIN_FAILURE);
            }

            User user = found.get();

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                throw new AuthException(GENERIC_LOGIN_FAILURE);
            }
            if (!user.isActive()) {
                throw new AuthException("This account has been deactivated.");
            }

            Role expected = request.getExpectedRole();
            if (expected != null && user.getRole() != expected) {
                throw new AuthException(
                        "This account is not a " + expected.name().toLowerCase(Locale.ROOT)
                        + " account. Use the correct sign-in page.");
            }

            return toSessionUser(user);
        });
    }

    /**
     * Converts a persisted user into the password-free session projection.
     *
     * @param user the persisted user
     * @return the session projection
     */
    public static SessionUser toSessionUser(final User user) {
        return new SessionUser(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());
    }

    private static String normaliseEmail(final String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireText(final String raw, final String label) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException(label + " is required.");
        }
        return raw.trim();
    }
}
