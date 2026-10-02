package com.vvmonitor.api.dto.response;

import java.time.Instant;

/** Token de acesso; enviar nas proximas requisicoes no header "Authorization: Bearer <accessToken>". */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

    public static LoginResponse bearer(String accessToken, Instant expiresAt, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresAt, user);
    }
}
