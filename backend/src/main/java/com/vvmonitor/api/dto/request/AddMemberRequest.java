package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** RF20: e-mail de quem vai ter acesso ao projeto (a pessoa ja precisa ter conta). */
public record AddMemberRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email
) {
}
