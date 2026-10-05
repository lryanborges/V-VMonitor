package com.vvmonitor.api.dto.request;

import com.vvmonitor.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;

/** RF14: o que pode ser editado num elemento. Tipo e codigo sao fixos. */
public record UpdateElementRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        String description,

        Priority priority
) {
}
