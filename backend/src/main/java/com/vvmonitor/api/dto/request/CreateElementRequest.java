package com.vvmonitor.api.dto.request;

import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * UC-05 e UC-07, passo 2. A prioridade e obrigatoria para requisitos e proibida para regras de negocio;
 * essa regra depende do tipo e e validada no ElementService.
 */
public record CreateElementRequest(

        @NotNull(message = "O tipo do elemento é obrigatório.")
        ElementKind kind,

        @NotBlank(message = "A descrição é obrigatória.")
        String description,

        Priority priority
) {
}
