package com.huyho.taskflow.auth;

import com.huyho.taskflow.auth.validation.PasswordByteLimit;
import com.huyho.taskflow.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(min = 12, message = "Password must be at least 12 characters")
        @PasswordByteLimit
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String password) {

    public RegisterRequest {
        if (email != null) email = User.normalizeEmail(email);
    }

    @Override
    public String toString() {
        return "RegisterRequest[credentials redacted]";
    }
}
