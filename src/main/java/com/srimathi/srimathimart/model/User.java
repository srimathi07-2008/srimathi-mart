package com.srimathi.srimathimart.model;

import java.sql.Timestamp;

/** A registered account. The password hash never leaves the service layer. */
public class User {

    private long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active = true;
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(final long value) {
        this.id = value;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String value) {
        this.fullName = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String value) {
        this.email = value;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(final String value) {
        this.passwordHash = value;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(final Role value) {
        this.role = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean value) {
        this.active = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt == null ? null : new Timestamp(createdAt.getTime());
    }

    public void setCreatedAt(final Timestamp value) {
        this.createdAt = value == null ? null : new Timestamp(value.getTime());
    }
}
