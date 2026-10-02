package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/** UC-01, passo 2: nome, e-mail, senha e confirmacao de senha. */
public record RegisterUserRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        // 72 e o limite de entrada do BCrypt
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String password,

        @NotBlank(message = "A confirmação de senha é obrigatória.")
        String passwordConfirmation
) {

    @AssertTrue(message = "A confirmação de senha não confere.")
    public boolean isPasswordConfirmed() {
        return password == null || passwordConfirmation == null
                || Objects.equals(password, passwordConfirmation);
    }
}
