package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/** UC-02, passo 2: login (e-mail) e senha. */
public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String password
) {
}
