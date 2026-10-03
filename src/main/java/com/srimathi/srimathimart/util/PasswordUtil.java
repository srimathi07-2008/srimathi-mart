package com.srimathi.srimathimart.util;

import com.srimathi.srimathimart.exception.ValidationException;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing. The application only ever persists the bcrypt output -
 * a plaintext password is never written to the database, a log, or a session.
 */
public final class PasswordUtil {

    /** Work factor. Each increment doubles the hashing cost. */
    private static final int DEFAULT_COST = 12;

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private PasswordUtil() {
    }

    /**
     * Hashes a raw password with a fresh random salt.
     *
     * @param rawPassword the plaintext password
     * @return a bcrypt hash, salt included
     */
    public static String hash(final String rawPassword) {
        validateStrength(rawPassword);
        int cost = Config.getInt("security.bcrypt.cost", DEFAULT_COST);
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }

    /**
     * Checks a candidate password against a stored hash. Returns false rather
     * than throwing on a malformed hash so a corrupt row cannot crash login.
     *
     * @param rawPassword  the plaintext candidate
     * @param passwordHash the stored bcrypt hash
     * @return true when the password matches
     */
    public static boolean matches(final String rawPassword, final String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Enforces the minimum password policy.
     *
     * @param rawPassword the plaintext password
     */
    public static void validateStrength(final String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (rawPassword.length() < MIN_LENGTH) {
            throw new ValidationException(
                    "Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LENGTH) {
            throw new ValidationException(
                    "Password must be at most " + MAX_LENGTH + " bytes.");
        }
    }
}
