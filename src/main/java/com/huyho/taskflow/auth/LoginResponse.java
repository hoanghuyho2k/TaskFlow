package com.huyho.taskflow.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, String displayName) {
    @Override public String toString() { return "LoginResponse[token redacted]"; }
}
