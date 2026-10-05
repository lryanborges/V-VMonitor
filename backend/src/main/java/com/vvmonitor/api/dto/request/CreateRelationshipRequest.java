package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** UC-06: origem (source), destino (target) e tipo. Le-se "origem [tipo] destino", ex.: RF7 depende de RF5. */
public record CreateRelationshipRequest(

        @NotNull(message = "O elemento de origem é obrigatório.")
        UUID sourceId,

        @NotNull(message = "O elemento de destino é obrigatório.")
        UUID targetId,

        @NotNull(message = "O tipo de relacionamento é obrigatório.")
        UUID typeId
) {
}
