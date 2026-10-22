package com.huyho.taskflow.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthenticatedUser {
    private AuthenticatedUser() {}

    public static Long id(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Long.valueOf(jwt.getToken().getSubject());
        }
        // Mock users in API tests represent the corresponding numeric JWT subject.
        return Long.valueOf(authentication.getName());
    }
}
