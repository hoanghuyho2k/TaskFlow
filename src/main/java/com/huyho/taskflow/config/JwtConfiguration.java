package com.huyho.taskflow.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;

@Configuration
public class JwtConfiguration {
    public static final String ISSUER = "taskflow";
    public static final String AUDIENCE = "taskflow-api";

    @Bean
    public SecretKey jwtSigningKey(@Value("${taskflow.jwt.secret}") String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must encode at least 32 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(AUDIENCE)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() != null && jwt.getSubject().matches("[1-9][0-9]*")
                && jwt.getExpiresAt() != null ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), audience, subject));
        return decoder;
    }
}
