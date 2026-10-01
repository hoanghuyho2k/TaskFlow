package com.huyho.taskflow.auth;

import java.time.Instant;
import com.huyho.taskflow.user.User;

public record UserResponse(Long id, String email, String displayName, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
