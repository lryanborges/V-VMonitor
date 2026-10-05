package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC-06, sequencia alternativa, passo 7: o ator informa o nome do novo relacionamento. */
public record CreateRelationshipTypeRequest(

        @NotBlank(message = "O nome do tipo de relacionamento é obrigatório.")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres.")
        String name
) {
}
