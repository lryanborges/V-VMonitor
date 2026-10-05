package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * RF14: troca o tipo e, se reversed, inverte a direcao (as pontas continuam as mesmas).
 * Em tipos simetricos a direcao nao importa e reversed e ignorado.
 */
public record UpdateRelationshipRequest(

        @NotNull(message = "O tipo de relacionamento é obrigatório.")
        UUID typeId,

        boolean reversed
) {
}
