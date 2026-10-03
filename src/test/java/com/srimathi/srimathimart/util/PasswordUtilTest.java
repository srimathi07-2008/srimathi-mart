package com.srimathi.srimathimart.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.srimathi.srimathimart.exception.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PasswordUtil}. */
class PasswordUtilTest {

    @BeforeAll
    static void useCheapCost() {
        // The production default of 12 would make this class take seconds.
        System.setProperty("security.bcrypt.cost", "4");
    }

    @Test
    @DisplayName("a hash verifies against its own password and nothing else")
    void hashAndVerify() {
        String hash = PasswordUtil.hash("Password@123");

        assertTrue(PasswordUtil.matches("Password@123", hash));
        assertFalse(PasswordUtil.matches("Password@124", hash));
        assertFalse(PasswordUtil.matches("", hash));
    }

    @Test
    @DisplayName("the hash never contains the plaintext")
    void hashHidesPlaintext() {
        String hash = PasswordUtil.hash("Password@123");

        assertNotEquals("Password@123", hash);
        assertFalse(hash.contains("Password@123"));
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    @DisplayName("the same password hashed twice gives different output")
    void saltIsRandom() {
        assertNotEquals(
                PasswordUtil.hash("Password@123"),
                PasswordUtil.hash("Password@123"));
    }

    @Test
    @DisplayName("a blank or corrupt stored hash can never be matched")
    void corruptHashIsSafe() {
        assertFalse(PasswordUtil.matches("Password@123", ""));
        assertFalse(PasswordUtil.matches("Password@123", null));
        assertFalse(PasswordUtil.matches("Password@123", "not-a-bcrypt-hash"));
    }

    @Test
    @DisplayName("an empty password in the seed rows cannot be logged into")
    void emptySeedHashRejected() {
        // seed.sql inserts password_hash = '' on purpose.
        assertFalse(PasswordUtil.matches("anything at all", ""));
    }

    @Test
    @DisplayName("the password policy is enforced")
    void policy() {
        assertThrows(ValidationException.class, () -> PasswordUtil.hash(null));
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("   "));
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("short12"));

        assertThrows(ValidationException.class,
                () -> PasswordUtil.hash("x".repeat(73)));

        // Exactly at the minimum is fine.
        assertTrue(PasswordUtil.hash("12345678").startsWith("$2"));
    }
}
