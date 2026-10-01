package com.huyho.taskflow.auth;

import com.huyho.taskflow.auth.validation.PasswordByteLimit;
import com.huyho.taskflow.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @PasswordByteLimit @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String password) {
    public LoginRequest {
        if (email != null) email = User.normalizeEmail(email);
    }
    @Override public String toString() { return "LoginRequest[credentials redacted]"; }
}
