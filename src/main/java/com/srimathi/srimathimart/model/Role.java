package com.srimathi.srimathimart.model;

/**
 * Account roles. ADMIN is created only by the seed listener - there is no
 * admin self-registration path anywhere in the application.
 */
public enum Role {

    BUYER,
    SELLER,
    ADMIN;

    /**
     * Parses a role name, returning null when the value is unknown.
     *
     * @param value raw role text, may be null
     * @return the matching role or null
     */
    public static Role from(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Roles a visitor is allowed to self register as.
     *
     * @param value raw role text
     * @return true when the role is BUYER or SELLER
     */
    public static boolean isSelfRegisterable(final Role value) {
        return value == BUYER || value == SELLER;
    }
}
