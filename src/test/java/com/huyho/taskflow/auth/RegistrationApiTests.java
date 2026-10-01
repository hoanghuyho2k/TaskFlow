package com.huyho.taskflow.auth;

import java.util.Map;
import com.huyho.taskflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationApiTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper mapper;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private String body(String email, String name, String password) {
        return mapper.writeValueAsString(Map.of("email", email, "displayName", name, "password", password));
    }

    @Test
    void registersNormalizedUserAndStoresHashOnly() throws Exception {
        String raw = " Example-password-123! ";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(" REGISTER-TEST@EXAMPLE.COM ", " Learner ", raw)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("register-test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Learner"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        entityManager.clear();
        var stored = users.findByEmail("register-test@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("{bcrypt}").isNotEqualTo(raw);
        assertThat(passwords.matches(raw, stored.getPasswordHash())).isTrue();
        assertThat(passwords.matches(raw.strip(), stored.getPasswordHash())).isFalse();
    }

    @Test
    void duplicateEmailReturnsConflictWithoutAddingUser() throws Exception {
        String request = body("duplicate-registration@example.com", "Learner", "Example-password-123!");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        long count = users.count();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(" DUPLICATE-REGISTRATION@EXAMPLE.COM ", "Other", "Other-password-123!")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(users.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "            "})
    void invalidPasswordsDoNotWriteUsers(String password) throws Exception {
        long count = users.count();
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("invalid-password@example.com", "Learner", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("rejectedValue");
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void enforcesUtf8ByteLimitAndAcceptsBoundary() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("boundary@example.com", "Learner", "a".repeat(72))))
                .andExpect(status().isCreated());
        for (String password : new String[]{"a".repeat(73), "é".repeat(37)}) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(body("oversized@example.com", "Learner", password)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("Password must be at most 72 UTF-8 bytes"));
        }
    }

    @Test
    void validatesEmailNameAndMissingFields() throws Exception {
        for (String request : new String[]{"{}", body("invalid", "Learner", "Example-password-123!"),
                body("valid@example.com", "   ", "Example-password-123!"),
                body("valid@example.com", "a".repeat(101), "Example-password-123!")}) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void requestStringAndSerializationDoNotExposePassword() {
        String raw = "Example-password-123!";
        RegisterRequest request = new RegisterRequest("learner@example.com", "Learner", raw);
        assertThat(request.toString()).doesNotContain(raw);
        assertThat(mapper.writeValueAsString(request)).doesNotContain(raw, "password");
    }
}
