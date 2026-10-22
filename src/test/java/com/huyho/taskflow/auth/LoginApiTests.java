package com.huyho.taskflow.auth;

import java.time.Instant;
import java.util.List;
import com.huyho.taskflow.user.User;
import com.huyho.taskflow.user.UserRepository;
import com.huyho.taskflow.config.JwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class LoginApiTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder decoder;
    @Autowired private ObjectMapper mapper;

    @Test
    void loginIssuesTokenAcceptedByProtectedEndpoint() throws Exception {
        User user = users.saveAndFlush(new User("login-test@example.com", "Learner", passwords.encode("Example-password-123!")));
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" LOGIN-TEST@EXAMPLE.COM \",\"password\":\"Example-password-123!\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.displayName").value("Learner"))
                .andExpect(jsonPath("$.expiresIn").value(900)).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("accessToken").asText();
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(body).doesNotContain("passwordHash", "Example-password-123!");
    }

    @Test
    void missingAndWrongCredentialsHaveSameResponse() throws Exception {
        users.saveAndFlush(new User("known-login@example.com", "Learner", passwords.encode("Example-password-123!")));
        for (String email : new String[]{"known-login@example.com", "absent-login@example.com"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"Incorrect-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        }
    }

    private String token(String issuer, String audience, Instant expiry) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("14").issuer(issuer).audience(List.of(audience))
                .issuedAt(expiry.minusSeconds(900)).expiresAt(expiry).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void rejectsMissingMalformedExpiredWrongIssuerAndWrongAudienceTokens() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        for (String token : new String[]{"invalid", token(JwtConfiguration.ISSUER, JwtConfiguration.AUDIENCE, Instant.now().minusSeconds(120)),
                token("other", JwtConfiguration.AUDIENCE, Instant.now().plusSeconds(900)),
                token(JwtConfiguration.ISSUER, "other", Instant.now().plusSeconds(900))}) {
            mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
        }
    }

    @Test
    void rejectsTamperedSignatureAndInvalidLoginBody() throws Exception {
        String valid = token(JwtConfiguration.ISSUER, JwtConfiguration.AUDIENCE, Instant.now().plusSeconds(900));
        int index = valid.lastIndexOf('.') + 1;
        String tampered = valid.substring(0,index) + (valid.charAt(index) == 'A' ? 'B' : 'A') + valid.substring(index+1);
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
