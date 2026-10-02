package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.User;

import java.time.Instant;
import java.util.UUID;

/** Dados publicos do usuario; nunca expoe o hash da senha. */
public record UserResponse(UUID id, String name, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
