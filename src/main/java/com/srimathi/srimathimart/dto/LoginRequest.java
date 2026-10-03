package com.srimathi.srimathimart.dto;

import com.srimathi.srimathimart.model.Role;

/** Carries a login form from the controller into the service layer. */
public class LoginRequest {

    private final String email;
    private final String password;
    private final Role expectedRole;

    /**
     * Creates a login request.
     *
     * @param emailValue        login email
     * @param passwordValue     raw password
     * @param expectedRoleValue role the login page is for, or null for any
     */
    public LoginRequest(final String emailValue,
                        final String passwordValue,
                        final Role expectedRoleValue) {
        this.email = emailValue;
        this.password = passwordValue;
        this.expectedRole = expectedRoleValue;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getExpectedRole() {
        return expectedRole;
    }
}
