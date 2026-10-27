package com.vvmonitor.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** RF21: posicoes de nos arrastados no grafo (elementos ou testes), salvas em lote. */
public record SaveGraphPositionsRequest(
        @NotEmpty(message = "Informe ao menos uma posição.")
        @Size(max = 1000, message = "No máximo 1000 posições por vez.")
        List<@Valid NodePosition> positions
) {

    public record NodePosition(
            @NotNull(message = "O nó é obrigatório.") UUID nodeId,
            @NotNull(message = "A posição x é obrigatória.") Double x,
            @NotNull(message = "A posição y é obrigatória.") Double y
    ) {
    }
}
