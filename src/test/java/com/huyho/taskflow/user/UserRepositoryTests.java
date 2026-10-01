package com.huyho.taskflow.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTests {

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper mapper;

    @Test
    void persistsNormalizedUserAndVerifiablePasswordHash() {
        String raw = "Example-password-123!";
        String hash = passwords.encode(raw);
        Instant beforeSave = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Long id = users.saveAndFlush(new User("  Learner@Example.com  ", " Learner ", hash)).getId();
        entityManager.clear();

        User loaded = users.findByEmail("learner@example.com").orElseThrow();
        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getDisplayName()).isEqualTo("Learner");
        assertThat(loaded.getCreatedAt()).isBetween(beforeSave, Instant.now());
        assertThat(loaded.getPasswordHash()).isEqualTo(hash).isNotEqualTo(raw).startsWith("{bcrypt}");
        assertThat(passwords.matches(raw, loaded.getPasswordHash())).isTrue();
        assertThat(passwords.matches("Incorrect-password", loaded.getPasswordHash())).isFalse();
    }

    @Test
    void duplicateNormalizedEmailsAreRejectedByDatabase() {
        String hash = passwords.encode("Example-password-123!");
        users.saveAndFlush(new User("duplicate@example.com", "First", hash));
        assertThatThrownBy(() -> users.saveAndFlush(new User(" DUPLICATE@EXAMPLE.COM ", "Second", hash)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void passwordHashesAreExcludedFromJson() {
        String hash = passwords.encode("Example-password-123!");
        String json = mapper.writeValueAsString(new User("private@example.com", "Private", hash));
        assertThat(json).contains("private@example.com").doesNotContain("passwordHash", hash);
    }

    @Test
    void encodingUsesDifferentSaltsForSamePassword() {
        String raw = "Example-password-123!";
        String first = passwords.encode(raw);
        String second = passwords.encode(raw);
        assertThat(first).isNotEqualTo(second);
        assertThat(passwords.matches(raw, first)).isTrue();
        assertThat(passwords.matches(raw, second)).isTrue();
    }
}
