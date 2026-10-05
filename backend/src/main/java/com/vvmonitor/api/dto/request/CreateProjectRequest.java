package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC-03, passo 2: nome (obrigatorio) e descricao (opcional). */
public record CreateProjectRequest(

        @NotBlank(message = "O nome do projeto é obrigatório.")
        @Size(max = 150, message = "O nome do projeto deve ter no máximo 150 caracteres.")
        String name,

        String description
) {
}
