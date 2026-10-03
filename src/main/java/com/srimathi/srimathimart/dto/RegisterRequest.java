package com.srimathi.srimathimart.dto;

import com.srimathi.srimathimart.model.Role;

/** Carries a signup form from the controller into the service layer. */
public class RegisterRequest {

    private final String fullName;
    private final String email;
    private final String password;
    private final String confirmPassword;
    private final Role role;

    /**
     * Creates a signup request.
     *
     * @param fullNameValue        display name
     * @param emailValue           login email
     * @param passwordValue        raw password, hashed before storage
     * @param confirmPasswordValue repeated password
     * @param roleValue            BUYER or SELLER only
     */
    public RegisterRequest(final String fullNameValue,
                           final String emailValue,
                           final String passwordValue,
                           final String confirmPasswordValue,
                           final Role roleValue) {
        this.fullName = fullNameValue;
        this.email = emailValue;
        this.password = passwordValue;
        this.confirmPassword = confirmPasswordValue;
        this.role = roleValue;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public Role getRole() {
        return role;
    }
}
