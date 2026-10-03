package com.srimathi.srimathimart.dto;

import com.srimathi.srimathimart.model.Role;
import java.io.Serializable;

/**
 * The slim, password-free projection of a user that is stored in HttpSession.
 * Keeping the hash out of the session means it can never be serialised to disk
 * by Tomcat's session persistence.
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String fullName;
    private final String email;
    private final Role role;

    /**
     * Creates a session projection.
     *
     * @param idValue       user id
     * @param fullNameValue display name
     * @param emailValue    login email
     * @param roleValue     account role
     */
    public SessionUser(final long idValue,
                       final String fullNameValue,
                       final String emailValue,
                       final Role roleValue) {
        this.id = idValue;
        this.fullName = fullNameValue;
        this.email = emailValue;
        this.role = roleValue;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
