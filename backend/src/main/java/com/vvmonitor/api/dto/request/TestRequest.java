package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** RF9: descricao do teste e os requisitos (RF/RNF) que ele cobre; usado ao criar e ao editar. */
public record TestRequest(
        @NotBlank(message = "A descrição é obrigatória.")
        String description,

        @NotEmpty(message = "Escolha pelo menos um requisito coberto pelo teste.")
        List<UUID> elementIds
) {
}
