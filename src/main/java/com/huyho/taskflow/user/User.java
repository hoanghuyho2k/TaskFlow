package com.huyho.taskflow.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public static User reference(Long id) {
        User reference = new User();
        reference.id = Objects.requireNonNull(id, "User id is required");
        return reference;
    }

    // The caller must encode the password before constructing a User.
    public User(String email, String displayName, String passwordHash) {
        this.email = normalizeEmail(email);
        this.displayName = Objects.requireNonNull(displayName, "Display name is required").strip();
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash is required");
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "Email is required").strip().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    private void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
